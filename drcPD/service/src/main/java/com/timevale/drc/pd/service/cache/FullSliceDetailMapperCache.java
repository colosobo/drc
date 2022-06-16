package com.timevale.drc.pd.service.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.timevale.drc.base.dao.DrcSubTaskFullSliceDetailMapper;
import com.timevale.drc.base.model.DrcSubTaskFullSliceDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author gwk_2
 * @date 2022/4/15 10:16
 */
@Slf4j
@Component
public class FullSliceDetailMapperCache {

    @Autowired
    private DrcSubTaskFullSliceDetailMapper fullSliceDetailMapper;

    private LoadingCache<Integer, List<DrcSubTaskFullSliceDetail>> cache = CacheBuilder.newBuilder()
            //设置并发级别为8，并发级别是指可以同时写缓存的线程数
            .concurrencyLevel(8)
            //设置缓存容器的初始容量为10
            .initialCapacity(10)
            //设置缓存最大容量为n，超过n之后就会按照LRU最近虽少使用算法来移除缓存项
            .maximumSize(10000)
            //是否需要统计缓存情况,该操作消耗一定的性能,生产环境应该去除
            //.recordStats()
            //设置读写缓存后n秒钟过期,实际很少用到,类似于expireAfterWrite
            .expireAfterAccess(10, TimeUnit.SECONDS)
            //只阻塞当前数据加载线程，其他线程返回旧值
            //.refreshAfterWrite(13, TimeUnit.SECONDS)
            //设置缓存的移除通知
            .removalListener(notification -> {
                log.info(notification.getKey() + " " + notification.getValue() + " 被移除,原因:" + notification.getCause());
            })
            //build方法中可以指定CacheLoader，在缓存不存在时通过CacheLoader的实现自动加载缓存
            .build(new CacheLoader<Integer, List<DrcSubTaskFullSliceDetail>>() {
                @Override
                public List<DrcSubTaskFullSliceDetail> load(Integer id) throws Exception {
                    List<DrcSubTaskFullSliceDetail> sliceDetailList = fullSliceDetailMapper.selectByParentId(id);
                    return sliceDetailList;
                }
            });


    public List<DrcSubTaskFullSliceDetail> get(Integer id) {
        try {
            return fullSliceDetailMapper.selectByParentId(id);
        } catch (Exception e) {
            return null;
        }
    }

}
