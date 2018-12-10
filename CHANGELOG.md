## 0.5.1
* Adding a reference per API now only requires a text. If host and/or path are not given, the data sent via referer is added.

# 0.5
* Return all links instead of local ones for /links
* Use all links instead of local ones for arguments
* Refactor statements to have a list of `:references` instead of a single one. 
* Correctly handle `:predecessors` inside statements now
* Automatically fetch `:references` if statements are coming from D-BAS

## 0.4.1 
* Fix wrong adding of arguments

# 0.4
* Add route to fetch all arguments
* Add route to get arguments by a certain author (by name)

# 0.3
Major Changes with 0.3
* The user is not a simple String anymore. The user is now a map containing the public Screenname (`:name`), the `:id` and the `dgep-native` flag which is set when the user is native to the aggregator stated in `identifier.aggregator-id`.
* Shorthands for adding statements and whole arguments just by text all accept a author-id and assume the author is local to the current DGEP instance.
* Shorthands for adding arguments and statements now accept additional fields trhough the `additional` map. You can add any custom field there. Attributes added here will be first-class on the resulting statements. This means that adding `additional: {"foo" "bar"}` will add a field `:foo` with the value `bar` to the resulting statement.
* Added support for first-class reference search.
  * A `reference` field is expected to contain a `:text`, `:host` and `:path`
  * References can be searched by host- and text-content in the corresponding routes (See `/index.html#!/statements` for an overview)
* Other custom fields can be searched via `/statements/custom`
* A bug with the DBAS-Connector has been fixed and now again produces correct links.
