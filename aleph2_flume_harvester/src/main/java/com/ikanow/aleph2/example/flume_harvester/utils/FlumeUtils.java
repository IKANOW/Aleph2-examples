/*******************************************************************************
 * Copyright 2015, The IKANOW Open Source Project.
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
package com.ikanow.aleph2.example.flume_harvester.utils;

import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import com.codepoetics.protonpack.StreamUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.BucketUtils;
import com.ikanow.aleph2.data_model.utils.Lambdas;
import com.ikanow.aleph2.data_model.utils.Optionals;
import com.ikanow.aleph2.data_model.utils.UuidUtils;
import com.ikanow.aleph2.example.flume_harvester.data_model.FlumeBucketConfigBean;
import com.ikanow.aleph2.example.flume_harvester.data_model.FlumeBucketConfigBean.InputConfig.SpoolDirConfig;

/** Utilities for building Flume agent configurations
 * @author alex
 */
public class FlumeUtils {
	private static ObjectMapper _mapper = BeanTemplateUtils.configureMapper(Optional.empty());
	
	//////////////////////////////////////////////////////////////////////////////
	
	// CONFIGURATION
	
	/** Returns the morphlines config
	 * @param bucket_config
	 * @return
	 */
	public static Optional<String> createMorphlinesConfig(final FlumeBucketConfigBean bucket_config)
	{
		//TODO (ALEPH-10): security
		
		return Optional.of(Optional.ofNullable(bucket_config.morphlines_config_str()))
						.map(opt -> opt.map(ss -> ss + "\n").orElse(""))
						.map(s -> s + 
								Optional.ofNullable(bucket_config.morphlines_config())
										.map(o -> _mapper.convertValue(o, JsonNode.class)
											.toString())
										.orElse("")
							)
						.filter(s -> !s.isEmpty())
						;
	}
	
	/** Utility to get the list of spool dirs
	 * @param bucket_config
	 * @return
	 */
	public static Collection<SpoolDirConfig> getSpoolDirs(final FlumeBucketConfigBean bucket_config) {
		final Collection<SpoolDirConfig> dirs = Optional.ofNullable(bucket_config.input()).map(i -> Optionals.ofNullable(i.spool_dirs()))
				.orElse(Collections.emptyList())
				.stream()
				.filter(SpoolDirConfig::enabled) // (defaults to true)
				.collect(Collectors.toList())
				;		
		return dirs;
	}
	
	/** Utility to get the name of the test output dir
	 * @param bucket
	 * @return
	 */
	public static String getTestDirSuffix(final DataBucketBean bucket) {
		return BucketUtils.getUniqueSignature(bucket.full_name(), Optional.empty());
	}
	
	/** Utility to get the name of the tracking dir
	 * @param bucket
	 * @return
	 */
	public static String getTrackingDirSuffix(final DataBucketBean bucket) {
		return ".spool_" + bucket._id();
	}
	
	/** Utility to delete generate directories
	 * @param bucket
	 * @param spool_dirs
	 */
	public static void deleteGeneratedDirs(final DataBucketBean bucket, final FlumeBucketConfigBean bucket_config, boolean test_mode) {
		deleteGeneratedDirs(bucket, getSpoolDirs(bucket_config), test_mode);
	}
	
	/** Utility to delete generate directories
	 * @param bucket
	 * @param spool_dirs
	 */
	public static void deleteGeneratedDirs(final DataBucketBean bucket, final Collection<SpoolDirConfig> spool_dirs, boolean test_mode) {
		spool_dirs.stream()
			.forEach(v -> {
				if (test_mode) {
					FileUtils.deleteQuietly(new File(v.path() + "/" + getTestDirSuffix(bucket)));
				}
				FileUtils.deleteQuietly(new File(v.path() + "/" + getTrackingDirSuffix(bucket)));
			});
	}
	
	/** Auto-generates the flume config from an input block
	 *  If it's in test mode it also deletes the trackerDir (so this can be used for purging)
	 * @param bucket_config
	 * @param morphlines_config_path
	 * @param test_mode
	 * @return
	 */
	public static FlumeBucketConfigBean createAutoFlumeConfig(final DataBucketBean bucket, final FlumeBucketConfigBean bucket_config, final boolean test_mode) {
		//TODO (ALEPH-10): eventually add support for additiona short cuts here
		//TODO (ALEPH-10): security
		
		final Collection<SpoolDirConfig> dirs = getSpoolDirs(bucket_config);
		final AtomicInteger counter = new AtomicInteger(0); 
		
		if (!dirs.isEmpty()) {
			final ImmutableMap<String, String> new_flume_builder
				= dirs.stream()
						.reduce(
								ImmutableMap.<String, String>builder()
									// defaults
									.put("channels", "mem")
									.put("channels:mem:capacity", "1000")
									.put("channels:mem:transactionCapacity", "100")
									.put("channels:mem:type", "memory")
								,
								(acc, v) -> {
									final int count = counter.incrementAndGet();
									
									// (some tidy up that occurs in test mode)
									return Optional.<ImmutableMap.Builder<String,String>>of(acc
											.put("sources:file_in_" + count + ":type", "spooldir")
											.put("sources:file_in_" + count + ":channels", "mem")
											.put("sources:file_in_" + count + ":trackerDir", getTrackingDirSuffix(bucket))
											.put("sources:file_in_" + count + ":deletePolicy", (v.delete_on_ingest() ? "immediate" : "never"))
											.put("sources:file_in_" + count + ":spoolDir", 
													test_mode
													? v.path() + "/" + getTestDirSuffix(bucket)
													: v.path())
											.put("sources:file_in_" + count + ":ignorePattern", Optional.ofNullable(v.ignore_pattern()).orElse("^$")))
											// Some optional fields
											.map(acc2 -> {
												return Optional.ofNullable(v.append_basename_field())
																.map(field -> 
																	acc2.put("sources:file_in_" + count + ":basenameHeader", "true")
																		.put("sources:file_in_" + count + ":basenameHeaderKey", field)
																)
																.orElse(acc);
											})
											.map(acc2 -> {
												return Optional.ofNullable(v.append_path_field())
																.map(field -> 
																	acc2.put("sources:file_in_" + count + ":fileHeader", "true")
																		.put("sources:file_in_" + count + ":fileHeaderKey", field)
																)
																.orElse(acc);
											})
											.get()
											;
								}
								,
								(acc1, acc2) -> acc1 // (can't happen in practice)	
								)
								.put("sources", StreamUtils.zipWithIndex(dirs.stream()).map(i -> ("file_in_" + (1+i.getIndex()))).collect(Collectors.joining(" ")))
								.build();
								;

			// Clone the config with the new flume config
			return BeanTemplateUtils.clone(bucket_config).with(FlumeBucketConfigBean::flume_config, new_flume_builder).done();
		}
		else { // Leave unchanged
			return bucket_config;
		}		
	}
	
	
	/** Creates the flume config
	 * @param aagent_name
	 * @param bucket_config
	 * @param context_signature
	 * @param morphlines_config_path
	 * @return
	 */
	public static String createFlumeConfig(final String agent_name, 
											final DataBucketBean bucket,
											final FlumeBucketConfigBean bucket_config_in, 
											final String context_signature,
											final Optional<String> morphlines_config_path,
											final boolean test_mode)
	{
		//TODO (ALEPH-10): security
		final FlumeBucketConfigBean bucket_config = createAutoFlumeConfig(bucket, bucket_config_in, test_mode);
		
		// Handle test mode changes (a set of user overrides)
		final Map<String, String> flume_config = Lambdas.get(() -> {
			if (test_mode) { // overwrite using test overwrites
				return Stream.concat(
						Optional.ofNullable(bucket_config.flume_config()).orElse(Collections.emptyMap()).entrySet().stream(),
						Optional.ofNullable(bucket_config.flume_config_test_overrides()).orElse(Collections.emptyMap()).entrySet().stream()
					)
					.collect(
							() -> new HashMap<String, String>(), 
							(acc, kv) -> { 
								final String val = kv.getValue() instanceof String ? (String) kv.getValue() : null;
								acc.put(kv.getKey(), val); 
							},
							(acc1, acc2) ->  acc1.putAll(acc2)
							)
					;
			}
			else return bucket_config.flume_config();
		});
		
		
		final String sub_prefix = Optional.ofNullable(bucket_config.substitution_prefix()).orElse("$$$$");
		final String agent_prefix = agent_name + ".";
		
		final boolean sink_present = Optional.ofNullable(flume_config)
												.map(m -> m.containsKey("sinks"))
												.orElse(false);
				
		final String[] channels = Optional.ofNullable(flume_config)
									.map(m -> m.get("channels"))
									.map(c -> c.split("\\s+"))
									.orElse(new String[0]);
		
		//TODO (ALEPH-10 add to test case)
		if ((channels.length > 1) && !sink_present) {
			throw new RuntimeException("If have multiple channels then cannot use implicit sink");
		}
		
		//(not needed currently)
//		final Set<String> sinks = Optional.ofNullable(flume_config)
//											.map(m -> (String) m.get("sinks"))
//											.map(s -> Arrays.stream(s.split("\\s+"))
//															.collect(Collectors.toSet()))
//											.orElse(Collections.emptySet());
		
		return Optional.of(Optional.ofNullable(bucket_config.flume_config_str()))
						.map(opt -> opt.map(ss -> ss + "\n").orElse(""))
						.map(s -> s + 
								Optional.ofNullable(flume_config)
										.map(cfg -> cfg.entrySet().stream()
														.filter(kv -> null != kv.getValue()) // (fields nulled out by the test override)
														.map(kv -> agent_prefix 
																+ decodeKey(kv.getKey())
																+ "="
																+ decodeValue(kv.getValue(), sub_prefix, morphlines_config_path, context_signature)
														)
														.collect(Collectors.joining("\n"))
												)
										.filter(ss -> !ss.isEmpty())
										.orElse("")
						)										
						.map(s -> sink_present
									? s
									: (s + "\n"
										+ agent_prefix + "sinks=aleph2_sink"
											+ "\n"
										+ agent_prefix + "sinks.aleph2_sink."
											+ "channel=" + channels[0]
											+ "\n"
										+ agent_prefix + "sinks.aleph2_sink."
											+ "type=com.ikanow.aleph2.example.flume_harvester.services.FlumeHarvesterSink"
											+ "\n"
										+ agent_prefix + "sinks.aleph2_sink."
											+ "context_signature=" + encodeSignature(context_signature)
											+ "\n")
						)
						.get()
						;
	}
	
	public static String decodeValue(final String val, final String sub_prefix, Optional<String> morphline_cfg_path, String sig) {
		return val.replace(sub_prefix + "signature", encodeSignature(sig))
					.replace(sub_prefix + "morphline", morphline_cfg_path.orElse(""))
					.replace(sub_prefix + "hostname", HostInformationUtils.getHostname());
	}
	public static String decodeKey(final String key) {
		return key.replace(":", ".");
	}
	public static String encodeSignature(final String raw_sig) {
		return Lambdas.wrap_u(() -> URLEncoder.encode(raw_sig, "UTF-8")).get();
	}
	public static String decodeSignature(final String encoded_sig) {
		return Lambdas.wrap_u(() -> URLDecoder.decode(encoded_sig, "UTF-8")).get();
	}

	//////////////////////////////////////////////////////////////////////////////
	
	public static List<FlumeBucketConfigBean> getAgents(final DataBucketBean bucket) {
		return Optional.ofNullable(bucket.harvest_configs()).orElse(Collections.emptyList())
				.stream()
				.limit(1) //TODO (ALEPH-10): see updateAgentConfig only handle one at a time
				.filter(hcfg -> Optional.ofNullable(hcfg.enabled()).orElse(true)) // enabled
				.filter(hcfg -> null != hcfg.config()) // config enabled
				.map(hcfg -> BeanTemplateUtils.from(hcfg.config(), FlumeBucketConfigBean.class).get())
				.collect(Collectors.toList())
				;
	}
	
	//////////////////////////////////////////////////////////////////////////////
	
	// CONFIGURATION NAME
	
	private static final int MAX_CFG_COMP_LEN = 8;
	
	/** Returns the config name 
	 *  Generated by taking 1-3 directories from the path and then appening the end of a UUID
	 * @param path
	 * @return
	 */
	public static String getConfigName(final String path, final Optional<String> agent_name) {
		
		String[] components = Optional.of(path)
								.map(p -> p.startsWith("/") ? p.substring(1) : p)
								.get()
								.split("[/]");
		
		if (1 == components.length) {
			return tidyUpIndexName(safeTruncate(components[0], MAX_CFG_COMP_LEN)
										+ addOptionalAgentName(agent_name, MAX_CFG_COMP_LEN))
										+ "_" + generateUuidSuffix(path + agent_name.orElse(""));
		}
		else if (2 == components.length) {
			return tidyUpIndexName(safeTruncate(components[0], MAX_CFG_COMP_LEN) 
									+ "_" + safeTruncate(components[1], MAX_CFG_COMP_LEN)
									+ addOptionalAgentName(agent_name, MAX_CFG_COMP_LEN))
									+ "_" + generateUuidSuffix(path + agent_name.orElse(""));
		}
		else { // take the first and the last 2
			final int n = components.length;
			return tidyUpIndexName(safeTruncate(components[0], MAX_CFG_COMP_LEN) 
									+ "_" + safeTruncate(components[n-2], MAX_CFG_COMP_LEN) 
									+ "_" + safeTruncate(components[n-1], MAX_CFG_COMP_LEN) 
									+ addOptionalAgentName(agent_name, MAX_CFG_COMP_LEN))
									+ "_" + generateUuidSuffix(path + agent_name.orElse(""));
		}
	}
	// Utils for getBaseIndexName
	private static String addOptionalAgentName(final Optional<String> agent_name, final int max_len) {
		return agent_name.map(sc -> "_" + safeTruncate(sc, max_len)).orElse("");
	}
	private static String tidyUpIndexName(final String in) {
		return in.toLowerCase().replaceAll("[^a-z0-9_]", "_").replaceAll("__+", "_");
	}
	private static String generateUuidSuffix(final String in) {
		return UuidUtils.get().getContentBasedUuid(in.getBytes()).substring(24);
	}
	private static String safeTruncate(final String in, final int max_len) {
		return in.length() < max_len ? in : in.substring(0, max_len);
	}
}
