package com.timevale.drc.pd.service.full.resolve;

import com.timevale.drc.base.mysql.MySqlKeyUtil;
import com.timevale.drc.base.util.JdbcTemplateManager;
import com.timevale.drc.base.util.TaskNameBuilder;
import com.timevale.drc.pd.service.util.PkResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import static com.timevale.drc.pd.service.full.resolve.Slice.EMPTY;

/**
 * @author gwk_2
 * @date 2021/8/18 20:12
 * @description
 */
@Slf4j
public abstract class TableSplitResolver {
    private final JdbcTemplateManager jdbcTemplateManager = new JdbcTemplateManager();
    private final LinkedBlockingQueue<Slice> channel;

    public TableSplitResolver(LinkedBlockingQueue<Slice> channel) {
        this.channel = channel;
    }


    public <T> List<Slice> splitSlice(Integer drcSubTaskFullConfigId,
                                      Integer parentId,
                                      String parentName,
                                      String tableName,
                                        String url,
                                        String username,
                                        String pwd,
                                        String database,
                                        int rangeSize,
                                        int limit, String whereStatement){

        JdbcTemplate jdbcTemplate = jdbcTemplateManager.get(url, username, pwd, database);

        // 找到主键名称
        String pkName = jdbcTemplateManager.getPkFromTable(url, username, pwd, database, tableName);
        JdbcTemplateManager.FieldType fieldType = jdbcTemplateManager.getFieldType(jdbcTemplate, tableName, pkName);

        Map<String, String> minMax = getMinMax(pkName, tableName, whereStatement);
        Map.Entry<String, String> next = minMax.entrySet().iterator().next();

        RowMapper<T> resolver = new PkResolver().resolver(fieldType);
        T minPk = jdbcTemplate.queryForObject(next.getKey(), resolver);
        T maxPk = jdbcTemplate.queryForObject(next.getValue(), resolver);

        List<Slice> returnList = new ArrayList<>();

        T tmpMinPk = minPk;
        int tmpCount = 0;
        T rangeMinPkValue = minPk;
        int sliceNumber = 0;
        boolean first = true;
        while (true) {
            // log.info("TableWithWhereSplitResolver execute while true, table={}, tmpCount={}", tableName, tmpCount);
            // 控制第一次的数据获取.防止漏掉第一条.
            String symbol = ">";
            if (first) {
                symbol = ">=";
                first = false;
            }

            String sql = getSQL(tableName, limit, whereStatement, pkName, fieldType, maxPk, tmpMinPk, symbol);

            // 找到区间的数据.
            List<T> result = jdbcTemplate.query(sql, resolver);
            if (result.size() == 0 && tmpCount == 0) {
                if (returnList.size() == 0) {
                    // 空表.
                    Slice slider = createSlice(drcSubTaskFullConfigId, parentId, parentName, tableName, pkName, maxPk, tmpCount, rangeMinPkValue, sliceNumber, maxPk);
                    channel.offer(slider);
                }
                break;
            }

            T max = result.size() != 0 ? result.get(result.size() - 1) : maxPk;
            tmpMinPk = max;
            tmpCount += result.size();
            // 当此次循环已经达到 range 目标数量, 就生成一个新的 slice 对象, 重置.
            if (tmpCount >= rangeSize || max.equals(maxPk)) {
                Slice slice = createSlice(drcSubTaskFullConfigId, parentId, parentName, tableName, pkName, maxPk, tmpCount, rangeMinPkValue, sliceNumber, max);
                rangeMinPkValue = max;
                tmpCount = 0;
                sliceNumber += 1;
                boolean offer = channel.offer(slice);
                returnList.add(slice);
                if (!offer) {
                    throw new RuntimeException("offer queue fail.");
                }
                log.info("{} tmpCount = {}, sliceName = {}", tableName, slice.rangeSize, slice.sliceName);
            }
        }
        channel.offer(EMPTY);
        return null;
    }


    abstract  Map<String, String> getMinMax(String pkName, String tableName, String whereStatement) ;

    abstract <T> String getSQL(String tableName, int limit, String whereStatement, String pkName, JdbcTemplateManager.FieldType fieldType, T maxPk, T tmpMinPk, String symbol) ;


    protected <T> Slice createSlice(Integer drcSubTaskFullConfigId,
                                    Integer parentId, String parentName,
                                    String tableName, String pkName, T maxPk, int tmpCount,
                                    T rangeMinPkValue, int sliceNumber, T max) {

        Slice slice = new Slice();
        slice.sliceNumber = sliceNumber;
        slice.sliceName = TaskNameBuilder.buildFullName(parentName, MySqlKeyUtil.deConv(tableName), sliceNumber);
        slice.minPkValue = rangeMinPkValue.toString();
        slice.maxPkValue = max.toString();
        slice.rangeSize = tmpCount;
        slice.parentId = parentId;
        slice.slicePkName = pkName;
        slice.drcSubTaskFullConfigId = drcSubTaskFullConfigId;
        slice.isLastRange = max.equals(maxPk);
        return slice;
    }


}
