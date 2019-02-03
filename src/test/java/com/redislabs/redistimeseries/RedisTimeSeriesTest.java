package com.redislabs.redistimeseries;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.redislabs.redistimeseries.information.Info;
import com.redislabs.redistimeseries.information.Rule;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisTimeSeriesTest {

  private final JedisPool pool = new JedisPool();
  private final RedisTimeSeries client = new RedisTimeSeries(pool); 
  
  @Before
  public void testClient() {
    try (Jedis conn = pool.getResource()) {
      conn.flushAll();
    }      
  }
  
  @Test
  public void testCreate() {
    Assert.assertTrue(client.create("series1", 10/*retentionSecs*/, 10/*maxSamplesPerChunk*/));
    try (Jedis conn = pool.getResource()) {
      Assert.assertEquals("TSDB-TYPE", conn.type("series1"));
    }          
    
    try {
      Assert.assertTrue(client.create("series1", 10/*retentionSecs*/, 10/*maxSamplesPerChunk*/));
      Assert.fail();
    } catch(RedisTimeSeriesException e) {
    }
  }

  @Test
  public void testRule() {
    Assert.assertTrue(client.create("source", 10/*retentionSecs*/, 10/*maxSamplesPerChunk*/));
    Assert.assertTrue(client.create("dest", 10/*retentionSecs*/, 10/*maxSamplesPerChunk*/));
    
    Assert.assertTrue(client.createRule("source", Aggregation.AVG, 100, "dest"));
    
    try {
      Assert.assertFalse(client.createRule("source", Aggregation.COUNT, 100, "dest"));
      Assert.fail();
    } catch(RedisTimeSeriesException e) {
      // Error on creating same rule twice
    }
    
    Assert.assertTrue(client.deleteRule("source", "dest"));
    Assert.assertTrue(client.createRule("source", Aggregation.COUNT, 100, "dest"));
    
    try {
      Assert.assertTrue(client.deleteRule("source", "dest1"));
      Assert.fail();
    } catch(RedisTimeSeriesException e) {
      // Error on creating same rule twice
    }
  }
  
  @Test
  public void testAdd() {
    Assert.assertTrue(client.create("seriesAdd", 10/*retentionSecs*/, 10/*maxSamplesPerChunk*/));
    
    Assert.assertTrue(client.add("seriesAdd", 1000L, 1.1));
    Assert.assertTrue(client.add("seriesAdd", 3000L, 3.2));
    
    Value[] values = client.range("seriesAdd", 500L, 4000L, Aggregation.COUNT, 1);
    Assert.assertEquals(2, values.length);
    
    Assert.assertEquals( new Value(1000, 1), values[0]);
    Assert.assertEquals( new Value(3000, 1), values[1]);
    
    try {
      Assert.assertTrue(client.add("seriesAdd", 800L, 1.1));
      Assert.fail();
    } catch(RedisTimeSeriesException e) {
      // Error on creating same rule twice
    }
    
    try {
      client.range("seriesAdd1", 500L, 4000L, Aggregation.COUNT, 1);
      Assert.fail();
    } catch(RedisTimeSeriesException e) {
      // Error on creating same rule twice
    }
  }
  
  @Test
  public void testIncDec() {
    Assert.assertTrue(client.create("seriesIncDec", 100/*retentionSecs*/, 10/*maxSamplesPerChunk*/));   
    Assert.assertTrue(client.add("seriesIncDec", -1, 1));
    Assert.assertTrue(client.incrBy("seriesIncDec", 3, true, 10));
    Assert.assertTrue(client.decrBy("seriesIncDec", 2, true, 10));
    
    Value[] values = client.range("seriesIncDec", 1L, Long.MAX_VALUE, Aggregation.MAX, 10);
    Assert.assertEquals(1, values.length);
    Assert.assertEquals( 1, values[0].getValue(), 0);

    try {
      client.incrBy("seriesIncDec1", 3, true, 1);
      Assert.fail();
    } catch(RedisTimeSeriesException e) {
      // Error on creating same rule twice
    }
    
    try {
      client.decrBy("seriesIncDec1", 3, true, 1);
      Assert.fail();
    } catch(RedisTimeSeriesException e) {
      // Error on creating same rule twice
    }
  }
  
  @Test
  public void testInfo() {
    Assert.assertTrue(client.create("seriesInfo", 10/*retentionSecs*/, 10/*maxSamplesPerChunk*/));   

    Info info = client.info("seriesInfo");
    Assert.assertEquals( (Long)10L, info.getProperty("retentionSecs"));
    Assert.assertEquals( null, info.getLabel(""));
    Rule rule = info.getRule("");
//    Assert.assertEquals( "", rule);
//    Assert.assertEquals( "", rule.getTarget());
//    Assert.assertEquals( "", rule.getValue());
//    Assert.assertEquals( Aggregation.AVG, rule.getAggregation());

    
    try {
      client.info("seriesInfo1");
      Assert.fail();
    } catch(RedisTimeSeriesException e) {
      // Error on creating same rule twice
    }
  }


}
