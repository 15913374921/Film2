package com.example.partner.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Db;
import cn.hutool.db.DbUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.ds.simple.SimpleDataSource;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

/**
 * 代码生成器
 * v1.0
 */
@Slf4j
public class CodeGenerator {

    private static final String PACK_NAME = "com.example.partner";
    private static final String AUTHOR = "LZY";

    private static final String TABLE_NAME = "comments";

    private static final String VUE_CODE_PATH = "D:\\毕设\\amdin-vue\\src\\views\\";

    /*=========================  下面的不用改动  =========================*/

    private static final String PROJECT_PATH = System.getProperty("user.dir");
    public static final String MAPPER_XML_PATH = "/src/main/resources/mapper/";
    public static final String JAVA_CODE_PATH = "/src/main/java/";

    public static final String SPACE8 = "        ";

    public static void main(String[] args) {
        generateJava(TABLE_NAME);
        generateVue(TABLE_NAME);
    }

    private static void generateVue(String tableName){
        List<TableColumn> tableColumns = getTableColumn(tableName);

        // 读取模板，生成代码
        String vueTemplate = ResourceUtil.readUtf8Str("templates/vue.templates");
        Map<String ,String> map = new HashMap<>();
        map.put("lowerEntity", getLowerEntity(tableName));
        String vueTableBody = getVueTableBody(tableColumns);
        map.put("tableBody", vueTableBody);
        String vueFormBody = getVueFormBody(tableColumns);
        map.put("formBody",vueFormBody);
        String vuePage = StrUtil.format(vueTemplate, map);
        // vue工程文件目录
        FileUtil.writeUtf8String(vuePage,VUE_CODE_PATH + tableName + ".vue");

    }

    private static List<TableColumn> getTableColumn(String tableName) {
        DBProp dbProp = getDBProp();
        // 连接数据库
        String url = dbProp.getUrl();
        DataSource dataSource = new SimpleDataSource("jdbc:mysql://localhost:3306/information_schema",dbProp.getUsername(),dbProp.getPassword());
        Db db = DbUtil.use(dataSource);
        // 找到实际要查询的数据库名称
        String schema = url.substring(url.indexOf("3306/") + 5, url.indexOf("?"));
        List<TableColumn> tableColumns = new ArrayList<>();
        try {
            List<Entity> columns = db.findAll(Entity.create("COLUMNS").set("TABLE_SCHEMA",schema).set("TABLE_NAME",tableName));
            // 封装结构信息
            for (Entity entity : columns) {
                String columnName = entity.getStr("COLUMN_NAME"); // 字段名称
                String dataType = entity.getStr("DATA_TYPE"); // 字段
                String columnComment = entity.getStr("COLUMN_COMMENT");
                TableColumn tableColumn = TableColumn.builder().columnName(columnName).dataType(dataType).columnComment(columnComment).build();
                tableColumns.add(tableColumn);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return tableColumns;
    }

    private static String getVueTableBody(List<TableColumn> tableColumns) {
        StringBuilder builder = new StringBuilder();
        for (TableColumn tableColumn : tableColumns) {
            if(tableColumn.getColumnName().equalsIgnoreCase("id") && StrUtil.isBlank(tableColumn.getColumnComment())) {
                tableColumn.setColumnComment("编号");
            }
            if(tableColumn.getColumnName().equalsIgnoreCase("deleted") || tableColumn.getColumnName().equalsIgnoreCase("create_time") ||
                    tableColumn.getColumnName().equalsIgnoreCase("update_time")) {
                continue;
            }
            String colum = SPACE8 + "<el-table-column prop=\"" + tableColumn.getColumnName() + "\" label=\""
                    +tableColumn.getColumnComment() + "\" width=\"120\" align=\"center\" />\n";
            builder.append(colum);
        }
        return builder.toString();
    }

    private static String getVueFormBody(List<TableColumn> tableColumns) {
        StringBuilder builder = new StringBuilder();
        for (TableColumn tableColumn : tableColumns) {
            if(tableColumn.getColumnName().endsWith("id")) {
                continue;
            }
            if(tableColumn.getColumnName().equalsIgnoreCase("deleted") || tableColumn.getColumnName().equalsIgnoreCase("create_time") ||
                    tableColumn.getColumnName().equalsIgnoreCase("update_time")) {
                continue;
            }
            String colum = SPACE8 + "<el-form-item label=\"" + tableColumn.getColumnComment() + "\" prop=\"" + tableColumn.getColumnName() + "\">\n" +
                    SPACE8 + "          <el-input \n" +
                    SPACE8 + "            v-model=\"form." + tableColumn.getColumnName() + "\" \n" +
                    SPACE8 + "            placeholder=\"请输入" + tableColumn.getColumnComment() + "\"\n" +
                    SPACE8 + "            :disabled=\"form.id !== undefined\"\n" +
                    SPACE8 + "          />\n" +
                    SPACE8 + "       </el-form-item>";
            builder.append(colum);
        }
        return builder.toString();
    }

    private static String getLowerEntity(String tableName) {
        tableName = tableName.replaceAll("t_","").replaceAll("sys_","");
        String entity = StrUtil.toCamelCase(tableName);
        return entity.substring(0,1).toUpperCase() + entity.substring(1);
    }
    private static DBProp getDBProp() {
        ClassPathResource resource = new ClassPathResource("application.yml");
        YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
        yamlPropertiesFactoryBean.setResources(resource);
        Properties properties = yamlPropertiesFactoryBean.getObject();
        return DBProp.builder().url(properties.getProperty("spring.datasource.url"))
                .username(properties.getProperty("spring.datasource.username"))
                .password(properties.getProperty("spring.datasource.password")).build();
    }

    private static void generateJava(String tableName) {
        DBProp dbProp = getDBProp();
        FastAutoGenerator.create(dbProp.getUrl(),dbProp.getUsername(),dbProp.getPassword())
                .globalConfig(builder -> {
                    builder.author(AUTHOR) // 设置作者
                            .enableSwagger()
                            .disableOpenDir()
                            .outputDir(PROJECT_PATH + JAVA_CODE_PATH); // 指定输出目录
                })
                .packageConfig(builder -> {
                    builder.parent(PACK_NAME) // 设置父包名
                            .moduleName("") // 设置父包模块名
                            .pathInfo(Collections.singletonMap(OutputFile.xml, PROJECT_PATH + MAPPER_XML_PATH)); // 设置mapperXml生成路径
                })
                .strategyConfig(builder -> {
                    builder.controllerBuilder().fileOverride().enableRestStyle().enableHyphenStyle()
                            .serviceBuilder().fileOverride()
                            .mapperBuilder().fileOverride()
                            .entityBuilder().fileOverride().enableLombok()
                            .addTableFills(new Column("create_time", FieldFill.INSERT))
                            .addTableFills(new Column("update_time", FieldFill.INSERT_UPDATE));
                    builder.addInclude(tableName) // 设置需要生成的表名
                            .addTablePrefix("t_", "sys_"); // 设置过滤表前缀
                })
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .execute();
    }

}
