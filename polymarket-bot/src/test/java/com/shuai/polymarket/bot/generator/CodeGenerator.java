package com.shuai.polymarket.bot.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;

import java.util.Map;

/**
 * MyBatis-Plus code generator.
 * Usage: modify DB_URL / TABLES, then run main().
 * Generated files go to src/main/java and src/main/resources/mapper.
 */
public class CodeGenerator {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/polymarket?useSSL=false&serverTimezone=UTC";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "your_password";

    // Tables to generate; leave empty to generate all tables
    private static final String[] TABLES = {
            // e.g. "market_event", "order_book"
    };

    private static final String BASE_PACKAGE = "com.shuai.polymarket.bot";
    private static final String PROJECT_PATH = System.getProperty("user.dir");

    public static void main(String[] args) {
        FastAutoGenerator.create(DB_URL, DB_USERNAME, DB_PASSWORD)
                .globalConfig(builder -> builder
                        .author("shuai")
                        .outputDir(PROJECT_PATH + "/src/main/java")
                        .commentDate("yyyy-MM-dd")
                        .disableOpenDir()
                )
                .packageConfig(builder -> builder
                        .parent(BASE_PACKAGE)
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .controller("controller")
                        .pathInfo(Map.of(
                                OutputFile.xml, PROJECT_PATH + "/src/main/resources/mapper"
                        ))
                )
                .strategyConfig(builder -> {
                    var entityBuilder = builder
                            .addInclude(TABLES)
                            .entityBuilder()
                            .naming(NamingStrategy.underline_to_camel)
                            .columnNaming(NamingStrategy.underline_to_camel)
                            .enableLombok()
                            .enableTableFieldAnnotation()
                            .logicDeleteColumnName("deleted");

                    entityBuilder.mapperBuilder()
                            .enableMapperAnnotation()
                            .enableBaseResultMap()
                            .enableBaseColumnList();

                    entityBuilder.controllerBuilder()
                            .enableRestStyle();
                })
                .execute();
    }
}
