/*******************************************************************************
* Copyright 2015, The IKANOW Open Source Project.
* 
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License, version 3,
* as published by the Free Software Foundation.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Affero General Public License for more details.
* 
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
******************************************************************************/
package com.ikanow.aleph2.enrichment.utils.services;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;



import scala.Tuple2;



import com.fasterxml.jackson.databind.JsonNode;
import com.ikanow.aleph2.data_model.interfaces.data_analytics.IBatchRecord;
import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentBatchModule;
import com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentModuleContext;
import com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean;
import com.ikanow.aleph2.data_model.objects.data_import.EnrichmentControlMetadataBean;
import com.ikanow.aleph2.data_model.utils.BeanTemplateUtils;
import com.ikanow.aleph2.data_model.utils.SetOnce;
import com.ikanow.aleph2.enrichment.utils.data_model.SimpleRegexFilterBean;
import com.ikanow.aleph2.enrichment.utils.data_model.SimpleRegexFilterBean.RegexConfig;

/** Filters records by regex
 * @author Alex
 */
public class SimpleRegexFilterService implements IEnrichmentBatchModule {

	public static class InternalRegexConfig {
		public List<InternalRegexElement> elements() { return elements; }
		private List<InternalRegexElement> elements;
		
		public static class InternalRegexElement {
			public Pattern regex() { return regex; }
			public List<String> fields() { return fields; }
			
			private Pattern regex;
			private List<String> fields;
		}
	}
	final SetOnce<InternalRegexConfig> _regex_config = new SetOnce<>();
	final SetOnce<IEnrichmentModuleContext> _context = new SetOnce<>();
	
	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentBatchModule#onStageInitialize(com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentModuleContext, com.ikanow.aleph2.data_model.objects.data_import.DataBucketBean, com.ikanow.aleph2.data_model.objects.data_import.EnrichmentControlMetadataBean, boolean)
	 */
	@Override
	public void onStageInitialize(IEnrichmentModuleContext context,
			DataBucketBean bucket, EnrichmentControlMetadataBean control,
			final Tuple2<ProcessingStage, ProcessingStage> previous_next)
	{
		
		final SimpleRegexFilterBean config_bean = BeanTemplateUtils.from(Optional.ofNullable(control.config()).orElse(Collections.emptyMap()), SimpleRegexFilterBean.class).get();
		
		final InternalRegexConfig regex_config =
				BeanTemplateUtils.build(InternalRegexConfig.class)
					.with(InternalRegexConfig::elements,
							config_bean.elements().stream()
										.filter(element -> element.enabled())
										.map(element ->
											BeanTemplateUtils.build(InternalRegexConfig.InternalRegexElement.class)
												.with(InternalRegexConfig.InternalRegexElement::regex, buildRegex(element))
												.with(InternalRegexConfig.InternalRegexElement::fields, element.fields())
											.done().get()
										)
							.collect(Collectors.toList())
							)
				.done().get();
		
		_regex_config.trySet(regex_config);
		_context.trySet(context);
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentBatchModule#onObjectBatch(java.util.stream.Stream, java.util.Optional, java.util.Optional)
	 */
	@Override
	public void onObjectBatch(Stream<Tuple2<Long, IBatchRecord>> batch,
			Optional<Integer> batch_size, Optional<JsonNode> grouping_key) {
		
		batch.forEach(record -> {			
			final JsonNode record_json = record._2().getJson();
			boolean matched = false;
			final Iterator<InternalRegexConfig.InternalRegexElement> it_outer = _regex_config.get().elements().iterator();
			while (it_outer.hasNext() && !matched) {
				final InternalRegexConfig.InternalRegexElement element = it_outer.next();
				
				final Iterator<String> it_inner = element.fields().iterator();
				while (it_inner.hasNext() && !matched) {
					final String field = it_inner.next();
					final JsonNode j = record_json.get(field);
					if ((null != j) && j.isTextual()) {
						matched |= element.regex().matcher(j.asText()).find();
					}
				}
			}
			if (matched) {				
				_context.get().emitImmutableObject(record._1(), record_json, Optional.empty(), Optional.empty());
			}
		});
	}

	/* (non-Javadoc)
	 * @see com.ikanow.aleph2.data_model.interfaces.data_import.IEnrichmentBatchModule#onStageComplete(boolean)
	 */
	@Override
	public void onStageComplete(boolean is_original) {
		//(nothing to do)
	}

	/////////////////////////////////////////////////////////////////
	
	// UTILITIES
	
	/** Utility to build a regex out of a list of patterns
	 * @param config
	 * @return
	 */
	protected static Pattern buildRegex(final RegexConfig config) {
		final String regex = config.regexes().stream().map(s -> "(?:" + s + ")").collect(Collectors.joining("|"));		
		return Pattern.compile(regex, parseFlags(config.flags()));
	}
	
	/**
	 * Converts a string of regex flags into a single int representing those
	 * flags for using in the java Pattern object
	 * 
	 * @param flagsStr
	 * @return
	 */
	public static int parseFlags(final String flagsStr) {
		int flags = 0;
		for (int i = 0; i < flagsStr.length(); ++i) {
			switch (flagsStr.charAt(i)) {
			case 'i':
				flags |= Pattern.CASE_INSENSITIVE;
				break;
			case 'x':
				flags |= Pattern.COMMENTS;
				break;
			case 's':
				flags |= Pattern.DOTALL;
				break;
			case 'm':
				flags |= Pattern.MULTILINE;
				break;
			case 'u':
				flags |= Pattern.UNICODE_CASE;
				break;
			case 'd':
				flags |= Pattern.UNIX_LINES;
				break;
			}
		}
		return flags;
	}
}
