/*
 * -----------------------------------------------------------------------\
 * PerfCake
 *  
 * Copyright (C) 2010 - 2013 the original author or authors.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -----------------------------------------------------------------------/
 */
package org.perfcake.reporting.reporters.accumulators;

/**
 * Accumulates the maximum of values.
 * Atomic types are not used because both values must be set at the same time. Hence the methods are synchronized.
 *
 * @author Martin Večeřa <marvenec@gmail.com>
 *
 */
public class MaxLongValueAccumulator implements Accumulator<Long> {
   /**
    * Sum of the reported values
    */
   private long max = Long.MIN_VALUE;

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.reporting.reporters.accumulators.Accumulator#add(java.lang.Object)
    */
   @Override
   public synchronized void add(final Long number) {
      if (number > max) {
         max = number;
      }
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.reporting.reporters.accumulators.Accumulator#getResult()
    */
   @Override
   public synchronized Long getResult() {
      return max;
   }

   /*
    * (non-Javadoc)
    *
    * @see org.perfcake.reporting.reporters.accumulators.Accumulator#reset()
    */
   @Override
   public synchronized void reset() {
      max = Long.MIN_VALUE;
   }

}
