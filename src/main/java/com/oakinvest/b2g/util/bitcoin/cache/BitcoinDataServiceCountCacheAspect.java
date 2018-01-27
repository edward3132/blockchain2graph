package com.oakinvest.b2g.util.bitcoin.cache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

import static com.oakinvest.b2g.configuration.bitcoin.BitcoinConfiguration.BITCOIN_BLOCK_GENERATION_DELAY;

/**
 * Aspect adding a cache of BitcoinDataService count method.
 * Created by straumat on 30/06/17.
 */
@Configuration
@Aspect
public class BitcoinDataServiceCountCacheAspect {

    /**
     * How many milli seconds in 1 minute.
     */
    private static final float MILLISECONDS_IN_ONE_MINUTE = 60F * 1000F;

    /**
     * Last block count value.
     */
    private int lastBlockCountValue = -1;

    /**
     * Last block count access.
     */
    private long lastBlockCountValueAccess = 1L;

    /**
     * Cache for getBlockCountFromCache().
     *
     * @param pjp object
     * @return block count
     * @throws Throwable exception
     */
    @SuppressWarnings("unchecked")
    @Around("execution(* com.oakinvest.b2g.service.bitcoin.BitcoinDataService.getBlockCount())")
    public final Optional<Integer> getBlockCount(final ProceedingJoinPoint pjp) throws Throwable {
        float elapsedMinutesSinceLastCall = (System.currentTimeMillis() - lastBlockCountValueAccess) / MILLISECONDS_IN_ONE_MINUTE;

        // If getBlockcount has never been call or more than 10 minutes have passed.
        if (elapsedMinutesSinceLastCall > BITCOIN_BLOCK_GENERATION_DELAY) {
            Optional<Integer> blockCount = ((Optional<Integer>) pjp.proceed(new Object[]{}));
            blockCount.ifPresent(integer -> {
                lastBlockCountValue = integer;
                lastBlockCountValueAccess = System.currentTimeMillis();
            });
            return blockCount;
        } else {
            // Else we return what's in cache.
            return Optional.of(lastBlockCountValue);
        }
    }

}
