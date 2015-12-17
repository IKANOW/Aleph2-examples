output {
  if [sourceKey] == "_XXX_SOURCEKEY_XXX_" {
	    rotating_file {
	     	#remove_field => [ "sourceKey" ] 
	        path => '_XXX_TEMPPATH_XXX_'
	        final_path => '_XXX_FINALPATH_XXX_'
	        max_size => _XXX_MAX_SIZE_XXX_
	        segment_period => _XXX_SEGMENT_PERIOD_XXX_
	        flush_interval => _XXX_FLUSH_INTERVAL_XXX_
	    }
	}	    
}