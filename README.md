# linked-crunchbase

This repository provides
* the **code for a Linked Data API for [Crunchbase](https://data.crunchbase.com/)**. See the Java project in the subfolder [CrunchbaseWrapper](CrunchbaseWrapper/README.md).
* **instructions how to obtain a Crunchbase RDF data set** (i.e, knowledge graph) by crawling this Linked Data API. See the following.


## Instructions for Crawling CrunchBase
__Prerequisites__: You need to have an API key from Crunchbase (either for research purposes or for commercial interests). New API keys can be requested [here](https://data.crunchbase.com/docs/license-agreement).

### Overview
The following steps need to be performed:
1. Downloading the CSV files from the Crunchbase API website.
2. Converting the CSV files data into the RDF format to be readable by linked data tools.
3. Using our Crunchbase wrapper and a data integration/crawling tool, such as Linked Data-Fu, to perform the crawling.

In the following, we explain these steps in more detail. Generally, we first crawl the data concerning organizations, people, and other entity types except news. Then, the data about news are separately crawled to ensure that all data is retrieved (due to data provisioning constraints of Crunchbase). Since the Crunchbase API has increased its functionality recently, all steps can be crawled now also with the second method directly.

### Crawling Organizations etc.
#### Step 1: Downloading CSV Files
Firstly, the CSV node keys files need to be downloaded from the Crunchbase website (https://data.crunchbase.com/docs/node-export). The direct download link is as follows:

https://api.crunchbase.com/v3.1/node_keys/node_keys.tar.gz?user_key={user_key}

_user_key_ is thereby the Crunchbase API key of the user. In the downloaded compressed file, there is a CSV file for (almost) each entity type. As we exemplarily crawl organizations, we pick the file _organizations.csv_.
This CSV file includes a _NAME_ and _PERMALINK_ of all existing companies in Crunchbase. 

#### Step 2: Converting Data of CSV files to RDF
We use a program to convert the _permalinks_ in the CSV files to the RDF format. To that end, we use [this](crawling/CsvReader.java) Java class. The permalinks, which has the form _/organizations/facebook_ is converted into the form of
```
<http://linked-crunchbase.org/organizations/facebook#id><http://linked-crunchbase.org/api-vocab#api_path><http://linked-crunchbase.org/api/organizations/facebook?items_per_page=250> .
```
Note that the specification of 250 as _items_per_page_ is meant for indicating that 250 items should be returned for the given resource (here:  _/organizations/facebook_ ). In our evaluation, 250 was the highest number which was supported by the Crunchbase API. Using this parameter, we can reach 99.99% of the Crunchbase data. Only all data concerning news articles might not be reached with this parameter. In our Java implementation, we store the RDF data as an nt-file, which is used in the following steps.

#### Step 3: Running Linked Data-Fu
Using the nt file of the previous step, we use _Linked Data-Fu_ to crawl the Crunchbase data. [Linked Data Fu](https://linked-data-fu.github.io) can be requested from the developers.

The following command starts the Linked Data-Fu program for our crawling of organizations:
```
time linked-data-fu-0.9.12/bin/ldfu.sh -p get-organizations-crawling-rule.n3 -i output-of-java-program.nt -c ldfu.properties -o complete-organization-data.nt
```
The used files are described in the following:

- **get-organizations-crawling-rule.n3** is a N3 file that includes the rule which is used by Linked Data-Fu to understand how to follow the _api_path_ of organizations. An example configuration is given in the following:
```
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . 
@prefix foaf: <http://xmlns.com/foaf/0.1/> . 
@prefix http: <http://www.w3.org/2011/http#> . 
@prefix httpm: <http://www.w3.org/2011/http-methods#> . 
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . 
@prefix owl: <http://www.w3.org/2002/07/owl#> . 
@prefix dbpedia: <http://dbpedia.org/resource/> . 
@prefix dbo: <http://dbpedia.org/ontology/> . 
@prefix cb: <http://ontologycentral.com/2010/05/cb/vocab#> . 
@prefix cbw: <http://linked-crunchbase.org/api-vocab#> . 


{
  ?x cbw:api_path ?next .
} => {
  [] http:mthd httpm:GET ;
     http:requestURI ?next ;
	http:fieldName "Authorization" ;
	http:fieldValue "Basic Y3J1bmNoYmFzZSBpcyB0aGUga2V5" . 
} .
```

"Basic Y3J1bmNoYmFzZSBpcyB0aGUga2V5" is thereby the Base64 encoded version of the user's Crunchbase API key.
This N3 file is kept general and can be used for crawling also the other entity types.

- **output-of-java-program.nt** is the RDF file that we created in the step above and which serves now as input file. It includes the api_path's of all organizations.

- **ldfu.properties** includes the settings for controlling the delays and parallelism of requests to fulfill Crunchbase API limitations. We use the following configuration as content of ldfu.properties:
```
# timeouts managed by HttpClient
ldfu.http.connecttimeout=16000
ldfu.http.sockettimeout=32000
# how many parallel connections to one host
# managed by HttpClient; use in combination with
# ldfu.originqueue (rule of thumb: use originqueue=POLITE for
# maxroutesperhost == 1, originqueue=FIFO for maxroutesperhost > 1
ldfu.http.maxroutesperhost=1
# at what rate to do lookups; works only with originqueue=POLITE
# 1 request per crawlrate ms
ldfu.http.crawlrate=1
# number of retries for failing HTTP requests
ldfu.http.retries=8
ldfu.originqueue = POLITE
```
- **complete-organization-data.nt** is the output RDF file containing (hopefully) all Crunchbase data about organizations.

The outlined steps can be used to crawl also other entity types. In case there is no _permalink_ in the CSV file (e.g., acquisitions do not have any permalink, as a permalink is a natural-language naming but acquisitions do not have names), the _uuid_ should be used instead.

### Crawling News
For crawling all data represented about news articles, we use the following method. In this method, the parameter _key_set_url_ of the Crunchbase API is used. We directly use Linked Data-Fu without downloading any CSV file here. The below command should be run and uses again Linked Data-Fu:
```
time linked-data-fu-0.9.12/bin/ldfu.sh -p get-news-summary-using-keyseturl.n3  -c ldfu.properties -o all-news.nt
```
The files are identical to the previous crawling method. _all-news.nt_ is the output nt file. 
_get-news-summary-using-keyseturl.n3_ can be specified as follows:
```
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . 
@prefix foaf: <http://xmlns.com/foaf/0.1/> . 
@prefix http: <http://www.w3.org/2011/http#> . 
@prefix httpm: <http://www.w3.org/2011/http-methods#> . 
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> . 
@prefix owl: <http://www.w3.org/2002/07/owl#> . 
@prefix dbpedia: <http://dbpedia.org/resource/> . 
@prefix dbo: <http://dbpedia.org/ontology/> . 
@prefix cb: <http://ontologycentral.com/2010/05/cb/vocab#> . 
@prefix cbw: <http://linked-crunchbase.org/api-vocab#> .

{
  [] http:mthd httpm:GET ;
        http:requestURI 
<http://linked-crunchbase.org/api/news?items_per_page=250> ;
	http:fieldName "Authorization" ;
	http:fieldValue "Basic Y3J1bmNoYmFzZSBpcyB0aGUga2V5" . 
}

{
  ?x cbw:key_set_url ?next .
} => {
  [] http:mthd httpm:GET ;
     http:requestURI ?next ;
	http:fieldName "Authorization" ;
	http:fieldValue "Basic Y3J1bmNoYmFzZSBpcyB0aGUga2V5" . 
} .
```
"Basic Y3J1bmNoYmFzZSBpcyB0aGUga2V5" is again the Base64 encoding of the user's API key for Crunchbase. Note that this second method can be used for crawling the summary of other entity types as well. We did not do that so far, since Crunchbase' API  functionality for that was added at a later stage.

## Contributors
* Michael Färber, michael.faerber@kit.edu, Karlsruhe Institute of Technoogy (previously: University of Freiburg)
* Ali Aghaee, University of Freiburg
* Andreas Harth, Friedrich-Alexander-Universität Erlangen-Nürnberg

## Contact
michael.faerber@kit.edu

## Further Information and References
1. Michael Färber: [Linked Crunchbase: A Linked Data API and RDF Data Set About Innovative Companies](https://arxiv.org/pdf/1907.08671.pdf), arXiv preprint arXiv:1907.08671, 2019.
2. Michael Färber, Carsten Menne, Andreas Harth: [A Linked Data Wrapper for CrunchBase](http://semantic-web-journal.net/system/files/swj1493.pdf), Semantic Web Journal 9(4), IOS Press, 2018, pp. 505–5015.

_Last change: Aug 2019_
