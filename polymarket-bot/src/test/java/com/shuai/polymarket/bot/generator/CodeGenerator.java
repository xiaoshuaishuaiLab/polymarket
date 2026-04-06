package com.shuai.polymarket.bot.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * MyBatis-Plus code generator.
 * Usage: modify TABLES, then run main().
 * DB config is read from application-dev.yml.
 * Generated files go to src/main/java and src/main/resources/mapper.
 */
public class CodeGenerator {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/polymarket?useSSL=false&serverTimezone=UTC";
    private static final String DB_USERNAME;
    private static final String DB_PASSWORD;

    static {
        Yaml yaml = new Yaml();
        Path configPath = Path.of(System.getProperty("user.dir"), "polymarket-bot/src/main/resources/application-dev.yml");
        try (InputStream is = Files.newInputStream(configPath)) {
            Map<String, Object> data = yaml.load(is);
            @SuppressWarnings("unchecked")
            Map<String, Object> spring = (Map<String, Object>) data.get("spring");
            @SuppressWarnings("unchecked")
            Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");
            DB_USERNAME = (String) datasource.get("username");
            DB_PASSWORD = (String) datasource.get("password");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load application-dev.yml", e);
        }
    }

    // Tables to generate; leave empty to generate all tables
    private static final String[] TABLES = {
           "market"
    };

    private static final String BASE_PACKAGE = "com.shuai.polymarket.bot";
    private static final String PROJECT_PATH = System.getProperty("user.dir");

    public static void main(String[] args) {
        FastAutoGenerator.create(DB_URL, DB_USERNAME, DB_PASSWORD)
                .globalConfig(builder -> builder
                        .author("shuai")
                        .outputDir(PROJECT_PATH + "/polymarket-bot/src/main/java")
                        .commentDate("yyyy-MM-dd")
                        .disableOpenDir()
                )
                .packageConfig(builder -> builder
                        .parent(BASE_PACKAGE)
                        .entity("entity")
                        .mapper("mapper")
                        .pathInfo(Map.of(
                                OutputFile.xml, PROJECT_PATH + "/polymarket-bot/src/main/resources/mapper"
                        ))
                )
                .strategyConfig(builder -> {
                    builder
                            .addInclude(TABLES)
                            .entityBuilder()
                            .naming(NamingStrategy.underline_to_camel)
                            .columnNaming(NamingStrategy.underline_to_camel)
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .logicDeleteColumnName("deleted")
                            .mapperBuilder()
                            .enableMapperAnnotation()
                            .enableBaseResultMap()
                            .enableBaseColumnList()
                            .serviceBuilder()
                            .disable()  // Disable ITestService and TestServiceImpl generation
                            .controllerBuilder()
                            .disable(); // Disable TestController generation
                })
                .execute();
    }
}
