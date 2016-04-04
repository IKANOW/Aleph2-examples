/*******************************************************************************
 * Copyright 2016, The IKANOW Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.ikanow.aleph2.harvest.logstash.utils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import scala.Tuple2;

import com.google.common.collect.Sets;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IBasicMessageBeanSupplier;
import com.ikanow.aleph2.data_model.interfaces.shared_services.IBucketLogger;
import com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean;
import com.ikanow.aleph2.data_model.utils.ErrorUtils;

/**
 * @author Burch
 *
 */
public class TestLogstashUtils {

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test_sendOutputToLogger() throws IOException {
		final TestBucketLogger bucket_logger = new TestBucketLogger();		
		LogstashUtils.sendOutputToLogger(bucket_logger, Level.ERROR, new File("src/test/resources/ls_output.test"));
		assertEquals(66, bucket_logger.logged_messages.size());
	}
	
	@Test
	public void test_parseLogstashDate() throws ParseException {		
		final Set<String> test_dates = Sets.newHashSet("2016-03-24T09:38:03.770000-0400","2016-03-24T09:38:04.294000-0400");
		test_dates.stream().forEach(date_string -> {
			try {
				final Date parsed_date = LogstashUtils.parseLogstashDate(date_string);
				assertNotNull(parsed_date);
			} catch (Exception e) {
				e.printStackTrace();
				fail("failed to parse date: " + date_string);				
			}
		});
	}
	
	public class TestBucketLogger implements IBucketLogger {
		final Set<BasicMessageBean> logged_messages = new HashSet<BasicMessageBean>();
		
		@Override
		public CompletableFuture<?> flush() {
			return CompletableFuture.completedFuture(true);
		}

		/* (non-Javadoc)
		 * @see com.ikanow.aleph2.data_model.interfaces.shared_services.IBucketLogger#inefficientLog(org.apache.logging.log4j.Level, com.ikanow.aleph2.data_model.objects.shared.BasicMessageBean)
		 */
		@Override
		public CompletableFuture<?> inefficientLog(Level level,
				BasicMessageBean message) {
			logged_messages.add(message);
			System.out.println(ErrorUtils.show(message) + " " + message.details());
			return CompletableFuture.completedFuture(true);
		}

		/* (non-Javadoc)
		 * @see com.ikanow.aleph2.data_model.interfaces.shared_services.IBucketLogger#log(org.apache.logging.log4j.Level, com.ikanow.aleph2.data_model.interfaces.shared_services.IBasicMessageBeanSupplier)
		 */
		@Override
		public CompletableFuture<?> log(Level level,
				IBasicMessageBeanSupplier message) {
			logged_messages.add(message.getBasicMessageBean());
			System.out.println(ErrorUtils.show(message.getBasicMessageBean()));
			return CompletableFuture.completedFuture(true);
		}

		/* (non-Javadoc)
		 * @see com.ikanow.aleph2.data_model.interfaces.shared_services.IBucketLogger#log(org.apache.logging.log4j.Level, boolean, java.util.function.Supplier, java.util.function.Supplier)
		 */
		@Override
		public CompletableFuture<?> log(Level level, boolean success,
				Supplier<String> message, Supplier<String> subsystem) {
			// TODO Auto-generated method stub
			return null;
		}

		/* (non-Javadoc)
		 * @see com.ikanow.aleph2.data_model.interfaces.shared_services.IBucketLogger#log(org.apache.logging.log4j.Level, boolean, java.util.function.Supplier, java.util.function.Supplier, java.util.function.Supplier)
		 */
		@Override
		public CompletableFuture<?> log(Level level, boolean success,
				Supplier<String> message, Supplier<String> subsystem,
				Supplier<String> command) {
			// TODO Auto-generated method stub
			return null;
		}

		/* (non-Javadoc)
		 * @see com.ikanow.aleph2.data_model.interfaces.shared_services.IBucketLogger#log(org.apache.logging.log4j.Level, boolean, java.util.function.Supplier, java.util.function.Supplier, java.util.function.Supplier, java.util.function.Supplier)
		 */
		@Override
		public CompletableFuture<?> log(Level level, boolean success,
				Supplier<String> message, Supplier<String> subsystem,
				Supplier<String> command, Supplier<Integer> messageCode) {
			// TODO Auto-generated method stub
			return null;
		}

		/* (non-Javadoc)
		 * @see com.ikanow.aleph2.data_model.interfaces.shared_services.IBucketLogger#log(org.apache.logging.log4j.Level, boolean, java.util.function.Supplier, java.util.function.Supplier, java.util.function.Supplier, java.util.function.Supplier, java.util.function.Supplier)
		 */
		@Override
		public CompletableFuture<?> log(Level level, boolean success,
				Supplier<String> message, Supplier<String> subsystem,
				Supplier<String> command, Supplier<Integer> messageCode,
				Supplier<Map<String, Object>> details) {
			// TODO Auto-generated method stub
			return null;
		}

		/* (non-Javadoc)
		 * @see com.ikanow.aleph2.data_model.interfaces.shared_services.IBucketLogger#log(org.apache.logging.log4j.Level, com.ikanow.aleph2.data_model.interfaces.shared_services.IBasicMessageBeanSupplier, java.lang.String, java.util.Collection, java.util.Optional, java.util.function.BiFunction[])
		 */
		@Override
		public CompletableFuture<?> log(
				Level level,
				IBasicMessageBeanSupplier message,
				String merge_key,
				Collection<Function<Tuple2<BasicMessageBean, Map<String, Object>>, Boolean>> rule_functions,
				Optional<Function<BasicMessageBean, BasicMessageBean>> formatter,
				@SuppressWarnings("unchecked") BiFunction<BasicMessageBean, BasicMessageBean, BasicMessageBean>... merge_operations) {
			// TODO Auto-generated method stub
			return null;
		}

		/* (non-Javadoc)
		 * @see com.ikanow.aleph2.data_model.interfaces.shared_services.IBucketLogger#log(org.apache.logging.log4j.Level, com.ikanow.aleph2.data_model.interfaces.shared_services.IBasicMessageBeanSupplier, java.lang.String, java.util.function.BiFunction[])
		 */
		@Override
		public CompletableFuture<?> log(
				Level level,
				IBasicMessageBeanSupplier message,
				String merge_key,
				@SuppressWarnings("unchecked") BiFunction<BasicMessageBean, BasicMessageBean, BasicMessageBean>... merge_operations) {
			// TODO Auto-generated method stub
			return null;
		}

		/* (non-Javadoc)
		 * @see com.ikanow.aleph2.data_model.interfaces.shared_services.IBucketLogger#log(org.apache.logging.log4j.Level, com.ikanow.aleph2.data_model.interfaces.shared_services.IBasicMessageBeanSupplier, java.lang.String, java.util.function.Function, java.util.function.BiFunction[])
		 */
		@Override
		public CompletableFuture<?> log(
				Level level,
				IBasicMessageBeanSupplier message,
				String merge_key,
				Function<BasicMessageBean, BasicMessageBean> formatter,
				@SuppressWarnings("unchecked") BiFunction<BasicMessageBean, BasicMessageBean, BasicMessageBean>... merge_operations) {
			// TODO Auto-generated method stub
			return null;
		}

		/* (non-Javadoc)
		 * @see com.ikanow.aleph2.data_model.interfaces.shared_services.IBucketLogger#log(org.apache.logging.log4j.Level, com.ikanow.aleph2.data_model.interfaces.shared_services.IBasicMessageBeanSupplier, java.lang.String, java.util.Collection, java.util.function.BiFunction[])
		 */
		@Override
		public CompletableFuture<?> log(
				Level level,
				IBasicMessageBeanSupplier message,
				String merge_key,
				Collection<Function<Tuple2<BasicMessageBean, Map<String, Object>>, Boolean>> rule_functions,
				@SuppressWarnings("unchecked") BiFunction<BasicMessageBean, BasicMessageBean, BasicMessageBean>... merge_operations) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
