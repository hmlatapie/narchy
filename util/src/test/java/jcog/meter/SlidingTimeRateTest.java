package jcog.meter;

class SlidingTimeRateTest {

//        @Test
//        public void testUpdateAndGet()
//        {
//            SlidingTimeRate rate = new SlidingTimeRate(new TestTimeSource(), 10, 1, TimeUnit.SECONDS);
//            int updates = 100;
//            for (int i = 0; i < updates; i++)
//            {
//                rate.update(1);
//            }
//            Assert.assertEquals(updates, rate.get(TimeUnit.SECONDS), 0.0);
//        }
//
//        @Test
//        public void testUpdateAndGetBetweenWindows() throws InterruptedException
//        {
//            /Longsupplier time = new TestTimeSource();
//            SlidingTimeRate rate = new SlidingTimeRate(time, 5, 1, TimeUnit.SECONDS);
//            int updates = 100;
//            for (int i = 0; i < updates; i++)
//            {
//                rate.update(1);
//                time.sleep(100, TimeUnit.MILLISECONDS);
//            }
//            Assert.assertEquals(10, rate.get(TimeUnit.SECONDS), 0.0);
//        }
//
//        @Test
//        public void testUpdateAndGetPastWindowSize() throws InterruptedException
//        {
//            TestTimeSource time = new TestTimeSource();
//            SlidingTimeRate rate = new SlidingTimeRate(time, 5, 1, TimeUnit.SECONDS);
//            int updates = 100;
//            for (int i = 0; i < updates; i++)
//            {
//                rate.update(1);
//            }
//
//            time.sleep(6, TimeUnit.SECONDS);
//
//            Assert.assertEquals(0, rate.get(TimeUnit.SECONDS), 0.0);
//        }
//
//        @Test
//        public void testUpdateAndGetToPointInTime() throws InterruptedException
//        {
//            TestTimeSource time = new TestTimeSource();
//            SlidingTimeRate rate = new SlidingTimeRate(time, 5, 1, TimeUnit.SECONDS);
//            int updates = 10;
//            for (int i = 0; i < updates; i++)
//            {
//                rate.update(1);
//                time.sleep(100, TimeUnit.MILLISECONDS);
//            }
//
//            time.sleep(1, TimeUnit.SECONDS);
//
//            Assert.assertEquals(5, rate.get(TimeUnit.SECONDS), 0.0);
//            Assert.assertEquals(10, rate.get(1, TimeUnit.SECONDS), 0.0);
//        }
//
//        @Test
//        public void testDecay() throws InterruptedException
//        {
//            TestTimeSource time = new TestTimeSource();
//            SlidingTimeRate rate = new SlidingTimeRate(time, 5, 1, TimeUnit.SECONDS);
//            int updates = 10;
//            for (int i = 0; i < updates; i++)
//            {
//                rate.update(1);
//                time.sleep(100, TimeUnit.MILLISECONDS);
//            }
//            Assert.assertEquals(10, rate.get(TimeUnit.SECONDS), 0.0);
//
//            time.sleep(1, TimeUnit.SECONDS);
//
//            Assert.assertEquals(5, rate.get(TimeUnit.SECONDS), 0.0);
//
//            time.sleep(2, TimeUnit.SECONDS);
//
//            Assert.assertEquals(2.5, rate.get(TimeUnit.SECONDS), 0.0);
//        }
//
//        @Test
//        public void testPruning() throws InterruptedException
//        {
//            TestTimeSource time = new TestTimeSource();
//            SlidingTimeRate rate = new SlidingTimeRate(time, 5, 1, TimeUnit.SECONDS);
//
//            rate.update(1);
//            Assert.assertEquals(1, rate.size());
//
//            time.sleep(6, TimeUnit.SECONDS);
//
//            rate.prune();
//            Assert.assertEquals(0, rate.size());
//        }
//
//        @Test
//        public void testConcurrentUpdateAndGet() throws InterruptedException
//        {
//            final ExecutorService executor = Executors.newFixedThreadPool(FBUtilities.getAvailableProcessors());
//            final TestTimeSource time = new TestTimeSource();
//            final SlidingTimeRate rate = new SlidingTimeRate(time, 5, 1, TimeUnit.SECONDS);
//            int updates = 100000;
//            for (int i = 0; i < updates; i++)
//            {
//                executor.submit(() -> {
//                    time.sleep(1, TimeUnit.MILLISECONDS);
//                    rate.update(1);
//                });
//            }
//
//            executor.shutdown();
//
//            Assert.assertTrue(executor.awaitTermination(1, TimeUnit.MINUTES));
//            Assert.assertEquals(1000, rate.get(TimeUnit.SECONDS), 100.0);
//        }
//    }
}