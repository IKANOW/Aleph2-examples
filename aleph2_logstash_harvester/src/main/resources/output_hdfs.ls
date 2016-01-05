output {
  if [sourceKey] == "_XXX_SOURCEKEY_XXX_" {
	    webhdfs {
	     	#remove_field => [ "sourceKey" ] 
	        path => '_XXX_PATH_XXX_'
	        host => '_XXX_HOST_XXX_'
	        port => '_XXX_PORT_XXX_'
	        user => '_XXX_USER_XXX_'
	        codec => 'json'
	        flush_size => _XXX_FLUSH_SIZE_XXX_
	        idle_flush_time => _XXX_IDLE_FLUSH_TIME_XXX_
	    }
	}	    
}