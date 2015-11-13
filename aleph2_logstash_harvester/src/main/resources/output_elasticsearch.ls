output {
  if [sourceKey] == "_XXX_SOURCEKEY_XXX_" {
    elasticsearch_http {
	  remove_field => [ "sourceKey" ] 
      host => "localhost"
      #this was V1:
      #index => "recs_t__XXX_COMMUNITY_XXX__%{+YYYY.MM.dd}"
      index => "_XXX_INDEX_XXX_"
      template_overwrite => true
    }
  }
}
