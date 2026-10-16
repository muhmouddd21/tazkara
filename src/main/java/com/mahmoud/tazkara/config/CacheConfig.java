package com.mahmoud.tazkara.config;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Configuration;

@Configuration 
@EnableCaching 
public class CacheConfig implements CachingConfigurer{
    
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();   // log it, then treat it as a miss
    }
}
