output {
  if [sourceKey] == "_XXX_SOURCEKEY_XXX_" {
	    rotating_file {
	     	remove_field => [ "sourceKey" ] 
	        path => '_XXX_TEMPPATH_XXX_'
	        final_path => '_XXX_FINALPATH_XXX_'
	        max_size => 33554432
	        segment_period => 300
	        flush_interval => 60
	    }
	}	    
}