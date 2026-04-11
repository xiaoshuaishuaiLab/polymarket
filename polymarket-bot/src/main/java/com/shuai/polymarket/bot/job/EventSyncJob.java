package com.shuai.polymarket.bot.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shuai.polymarket.bot.entity.*;
import com.shuai.polymarket.bot.mapper.*;
import common.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

@Slf4j
@Component
public class EventSyncJob {

    private static final String API_PATH = "/events/keyset";
    private static final int LIMIT = 20;

    @Value("${polymarket.gamma.api-url}")
    private String apiBaseUrl;

    @Resource
    private EventMapper eventMapper;
    @Resource
    private MarketMapper marketMapper;
    @Resource
    private SeriesMapper seriesMapper;
    @Resource
    private TagMapper tagMapper;
    @Resource
    private EventTagMapper eventTagMapper;

    @Scheduled(fixedRate = 600_000)
    public void syncEvents() {
        log.info("EventSyncJob started");
        String afterCursor = null;
        boolean shouldStop = false;
        int totalSaved = 0;

        try {
            while (!shouldStop) {
                String url = buildUrl(afterCursor);
                log.info("Fetching events from: {}", url);

                var responseOpt = HttpUtil.get(url);
                log.info("responseOpt: {}", responseOpt);
                if (responseOpt.isEmpty()) {
                    log.warn("Empty response from API, stopping job");
                    break;
                }

                JSONObject resp = JSON.parseObject(responseOpt.get());
                JSONArray events = resp.getJSONArray("events");
                String nextCursor = resp.getString("next_cursor");

                if (events == null || events.isEmpty()) {
                    log.info("No events returned, stopping job");
                    break;
                }

                for (int i = 0; i < events.size(); i++) {
                    JSONObject eventJson = events.getJSONObject(i);
                    String eventId = eventJson.getString("id");

                    if (eventExistsInDb(eventId)) {
                        log.info("Event {} already exists in DB, stopping sync", eventId);
                        shouldStop = true;
                        break;
                    }

                    saveEvent(eventJson);
                    totalSaved++;
                }

                if (!shouldStop && nextCursor != null && !nextCursor.isBlank()) {
                    afterCursor = nextCursor;
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("EventSyncJob encountered an error", e);
        }

        log.info("EventSyncJob finished, saved {} new events", totalSaved);
    }

    private String buildUrl(String afterCursor) {
        var sb = new StringBuilder(apiBaseUrl)
                .append(API_PATH)
                .append("?limit=").append(LIMIT)
                .append("&ascending=false")
                .append("&order=startDate");
        if (afterCursor != null && !afterCursor.isBlank()) {
            sb.append("&after_cursor=").append(afterCursor);
        }
        return sb.toString();
    }

    private boolean eventExistsInDb(String eventId) {
        return eventMapper.selectCount(
                new LambdaQueryWrapper<Event>().eq(Event::getEventId, eventId)
        ) > 0;
    }

    // ==================== Save methods ====================

    private void saveEvent(JSONObject json) {
        String eventId = json.getString("id");

        // Save series first to get seriesId
        JSONArray seriesArray = json.getJSONArray("series");
        String seriesId = null;
        if (seriesArray != null && !seriesArray.isEmpty()) {
            JSONObject seriesJson = seriesArray.getJSONObject(0);
            seriesId = seriesJson.getString("id");
            saveSeries(seriesJson);
        }

        // Save event
        Event event = new Event();
        event.setEventId(eventId);
        event.setSeriesId(seriesId);
        event.setTicker(json.getString("ticker"));
        event.setSlug(json.getString("slug"));
        event.setTitle(json.getString("title"));
        event.setDescription(json.getString("description"));
        event.setResolutionSource(json.getString("resolutionSource"));
        event.setImage(json.getString("image"));
        event.setIcon(json.getString("icon"));
        event.setActive(json.getBoolean("active"));
        event.setClosed(json.getBoolean("closed"));
        event.setArchived(json.getBoolean("archived"));
        event.setIsNew(json.getBoolean("new"));
        event.setFeatured(json.getBoolean("featured"));
        event.setRestricted(json.getBoolean("restricted"));
        event.setLiquidity(parseBigDecimal(json, "liquidity"));
        event.setVolume(parseBigDecimal(json, "volume"));
        event.setOpenInterest(parseBigDecimal(json, "openInterest"));
        event.setCompetitive(parseBigDecimal(json, "competitive"));
        event.setVolume24hr(parseBigDecimal(json, "volume24hr"));
        event.setVolume1wk(parseBigDecimal(json, "volume1wk"));
        event.setVolume1mo(parseBigDecimal(json, "volume1mo"));
        event.setVolume1yr(parseBigDecimal(json, "volume1yr"));
        event.setEnableOrderBook(json.getBoolean("enableOrderBook"));
        event.setLiquidityClob(parseBigDecimal(json, "liquidityClob"));
        event.setNegRisk(json.getBoolean("negRisk"));
        event.setNegRiskMarketId(json.getString("negRiskMarketID"));
        event.setCommentCount(json.getInteger("commentCount"));
        event.setEventDate(parseLocalDate(json.getString("eventDate")));
        event.setStartDate(parseDateTime(json.getString("startDate")));
        event.setEndDate(parseDateTime(json.getString("endDate")));
        event.setCreatedAt(parseDateTime(json.getString("createdAt")));
        event.setUpdatedAt(parseDateTime(json.getString("updatedAt")));
        eventMapper.insert(event);

        // Save markets
        JSONArray markets = json.getJSONArray("markets");
        if (markets != null) {
            for (int i = 0; i < markets.size(); i++) {
                saveMarket(markets.getJSONObject(i), eventId);
            }
        }

        // Save tags and event_tag relations
        JSONArray tags = json.getJSONArray("tags");
        if (tags != null) {
            for (int i = 0; i < tags.size(); i++) {
                JSONObject tagJson = tags.getJSONObject(i);
                String tagId = tagJson.getString("id");
                saveTag(tagJson);
                saveEventTag(eventId, tagId);
            }
        }
    }

    private void saveSeries(JSONObject json) {
        String seriesId = json.getString("id");
        if (seriesId == null) return;

        boolean exists = seriesMapper.selectCount(
                new LambdaQueryWrapper<Series>().eq(Series::getSeriesId, seriesId)
        ) > 0;
        if (exists) return;

        Series series = new Series();
        series.setSeriesId(seriesId);
        series.setTicker(json.getString("ticker"));
        series.setSlug(json.getString("slug"));
        series.setTitle(json.getString("title"));
        series.setSeriesType(json.getString("seriesType"));
        series.setRecurrence(json.getString("recurrence"));
        series.setImage(json.getString("image"));
        series.setIcon(json.getString("icon"));
        series.setActive(json.getBoolean("active"));
        series.setClosed(json.getBoolean("closed"));
        series.setArchived(json.getBoolean("archived"));
        series.setVolume(parseBigDecimal(json, "volume"));
        series.setVolume24hr(parseBigDecimal(json, "volume24hr"));
        series.setLiquidity(parseBigDecimal(json, "liquidity"));
        series.setCommentCount(json.getInteger("commentCount"));
        series.setCreatedAt(parseDateTime(json.getString("createdAt")));
        series.setUpdatedAt(parseDateTime(json.getString("updatedAt")));
        seriesMapper.insert(series);
    }

    private void saveMarket(JSONObject json, String eventId) {
        String marketId = json.getString("id");
        if (marketId == null) return;

        boolean exists = marketMapper.selectCount(
                new LambdaQueryWrapper<Market>().eq(Market::getMarketId, marketId)
        ) > 0;
        if (exists) return;

        Market market = new Market();
        market.setMarketId(marketId);
        market.setEventId(eventId);
        market.setConditionId(json.getString("conditionId"));
        market.setQuestion(json.getString("question"));
        market.setSlug(json.getString("slug"));
        market.setResolutionSource(json.getString("resolutionSource"));
        market.setImage(json.getString("image"));
        market.setIcon(json.getString("icon"));
        market.setDescription(json.getString("description"));
        market.setOutcomes(json.getString("outcomes"));
        market.setOutcomePrices(json.getString("outcomePrices"));
        market.setClobTokenIds(json.getString("clobTokenIds"));
        market.setVolume(parseBigDecimal(json, "volume"));
        market.setLiquidity(parseBigDecimal(json, "liquidity"));
        market.setActive(json.getBoolean("active"));
        market.setClosed(json.getBoolean("closed"));
        market.setArchived(json.getBoolean("archived"));
        market.setIsNew(json.getBoolean("new"));
        market.setFeatured(json.getBoolean("featured"));
        market.setRestricted(json.getBoolean("restricted"));
        market.setEnableOrderBook(json.getBoolean("enableOrderBook"));
        market.setOrderPriceMinTickSize(parseBigDecimal(json, "orderPriceMinTickSize"));
        market.setOrderMinSize(parseBigDecimal(json, "orderMinSize"));
        market.setGroupItemTitle(json.getString("groupItemTitle"));
        market.setGroupItemThreshold(json.getString("groupItemThreshold"));
        market.setQuestionId(json.getString("questionID"));
        market.setNegRisk(json.getBoolean("negRisk"));
        market.setNegRiskMarketId(json.getString("negRiskMarketID"));
        market.setNegRiskRequestId(json.getString("negRiskRequestID"));
        market.setSpread(parseBigDecimal(json, "spread"));
        market.setBestBid(parseBigDecimal(json, "bestBid"));
        market.setBestAsk(parseBigDecimal(json, "bestAsk"));
        market.setLastTradePrice(parseBigDecimal(json, "lastTradePrice"));
        market.setOneHourPriceChange(parseBigDecimal(json, "oneHourPriceChange"));
        market.setCompetitive(parseBigDecimal(json, "competitive"));
        market.setUmaBond(json.getString("umaBond"));
        market.setUmaReward(json.getString("umaReward"));
        market.setFeeType(json.getString("feeType"));
        Object feeSchedule = json.get("feeSchedule");
        if (feeSchedule != null) {
            market.setFeeSchedule(JSON.toJSONString(feeSchedule));
        }
        market.setAcceptingOrders(json.getBoolean("acceptingOrders"));
        market.setStartDate(parseDateTime(json.getString("startDate")));
        market.setEndDate(parseDateTime(json.getString("endDate")));
        market.setCreatedAt(parseDateTime(json.getString("createdAt")));
        market.setUpdatedAt(parseDateTime(json.getString("updatedAt")));
        marketMapper.insert(market);
    }

    private void saveTag(JSONObject json) {
        String tagId = json.getString("id");
        if (tagId == null) return;

        boolean exists = tagMapper.selectCount(
                new LambdaQueryWrapper<Tag>().eq(Tag::getTagId, tagId)
        ) > 0;
        if (exists) return;

        Tag tag = new Tag();
        tag.setTagId(tagId);
        tag.setLabel(json.getString("label"));
        tag.setSlug(json.getString("slug"));
        tag.setForceShow(json.getBoolean("forceShow"));
        tag.setIsCarousel(json.getBoolean("isCarousel"));
        tag.setPublishedAt(parseDateTimeLoose(json.getString("publishedAt")));
        tag.setCreatedAt(parseDateTime(json.getString("createdAt")));
        tag.setUpdatedAt(parseDateTime(json.getString("updatedAt")));
        tagMapper.insert(tag);
    }

    private void saveEventTag(String eventId, String tagId) {
        if (eventId == null || tagId == null) return;

        boolean exists = eventTagMapper.selectCount(
                new LambdaQueryWrapper<EventTag>()
                        .eq(EventTag::getEventId, eventId)
                        .eq(EventTag::getTagId, tagId)
        ) > 0;
        if (exists) return;

        EventTag eventTag = new EventTag();
        eventTag.setEventId(eventId);
        eventTag.setTagId(tagId);
        eventTag.setCreatedAt(LocalDateTime.now());
        eventTagMapper.insert(eventTag);
    }

    // ==================== Parse helpers ====================

    private BigDecimal parseBigDecimal(JSONObject json, String key) {
        Object val = json.get(key);
        if (val == null) return null;
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse datetime: {}", value);
            return null;
        }
    }

    // Handles PostgreSQL format: "2023-11-02 21:15:31.924+00" (space separator, short timezone offset)
    private static final DateTimeFormatter POSTGRES_DATETIME_FORMATTER =
            new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd HH:mm:ss")
                    .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
                    .appendPattern("x")
                    .toFormatter();

    private LocalDateTime parseDateTimeLoose(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value, POSTGRES_DATETIME_FORMATTER).toLocalDateTime();
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse datetime (loose): {}", value);
            return null;
        }
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", value);
            return null;
        }
    }
}
