/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2017 by Hitachi Vantara : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.core.xml;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

//import java.util.Timer;
//import java.util.TimerTask;
//import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Singleton to help speed up lookups in an XML DOM tree.<br>
 * The theory is that you often loop over occurrences of a certain tag in a Node.<br>
 * If there are 20 occurrences, you go from index 0..19.<br>
 * Every time we do the following<br>
 * - found node 0<br>
 * - found node 0, 1<br>
 * - found node 0, 1, 2<br>
 * - ...<br>
 * So the time to search node index 19 is 20 times larger on average then index 0.<br>
 * <br>
 * We can solve this by caching the position of index 18 and by starting back at that position.<br>
 * <br>
 * This class is a singleton to keep everyting 100% compatible with the rest of the codebase. <br>
 *
 * @author Matt
 * @since 22-Apr-2006
 */
public class XMLHandlerCache {
//  public static final int MAX_NUMBER_OF_ENTRIES = 500;

  private static XMLHandlerCache instance;

  Cache<XMLHandlerCacheEntry, Integer> cache;

//  private ConcurrentLinkedQueue<XMLHandlerCacheEntry> orderReferenceQueue;

  private volatile int cacheHits;

//  Timer timer;

  private XMLHandlerCache() {
    cache = CacheBuilder.newBuilder().weakKeys().build();
//    orderReferenceQueue = new ConcurrentLinkedQueue<>();
//    timer = new Timer( XMLHandlerCache.class.getName() + " checking cache size task" );
//    timer.schedule( new TimerTask() {
//      @Override public void run() {
//        long momentSize = cache.size();
//        if ( momentSize > MAX_NUMBER_OF_ENTRIES ) {
//          for ( int i = 0; i < momentSize - MAX_NUMBER_OF_ENTRIES; i++ ) {
//            XMLHandlerCacheEntry cacheEntry = orderReferenceQueue.poll();
//            if ( cacheEntry != null ) {
//              cache.invalidate( orderReferenceQueue.poll() );
//            }
//          }
//        }
//      }
//    }, 0L, 30000L );
    cacheHits = 0;
  }

  public static synchronized XMLHandlerCache getInstance() {
    if ( instance == null ) {
      return instance = new XMLHandlerCache();
    }
    return instance;
  }

  /**
   * Store a cache entry
   *
   * @param entry
   *          The cache entry to store
   */
  public void storeCache( XMLHandlerCacheEntry entry, int lastChildNr ) {
    cache.put( entry, lastChildNr );
//    orderReferenceQueue.add( entry );
  }

  /**
   * Retrieve the last child were we left off...
   *
   * @param entry
   *          The cache entry to look for.
   * @return the last child position or -1 if nothing was found.
   */
  public int getLastChildNr( XMLHandlerCacheEntry entry ) {
    Integer lastChildNr = cache.getIfPresent( entry );
    if ( lastChildNr != null ) {
      cacheHits++;
      return lastChildNr;
    }
    return -1;
  }

  /**
   * @return the number of cache hits for your statistical pleasure.
   */
  public int getCacheHits() {
    return cacheHits;
  }

  /**
   * Allows you to (re-)set the number of cache hits
   *
   * @param cacheHits
   *          the number of cache hits.
   */
  public void setCacheHits( int cacheHits ) {
    this.cacheHits = cacheHits;
  }

  /**
   * Clears the cache
   *
   */
  public void clear() {
    cache.invalidateAll();
//    orderReferenceQueue.clear();
  }
}
