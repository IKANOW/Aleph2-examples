output {
  if [@metadata][sourceKey] == "_XXX_SOURCEKEY_XXX_" {
  	#todo we need to remove this field eventually  	
  	#remove_field => [ "sourceKey" ]
    elasticsearch {	   
      hosts => ["127.0.0.1:9200"]
      #this was V1:
      #index => "recs_t__XXX_COMMUNITY_XXX__%{+YYYY.MM.dd}"
      index => "_XXX_INDEX_XXX_"
      #template_overwrite => true
    }
  }
}
