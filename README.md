# Reviews service
Http service for data analysis on customer reviews.

## Contents
* [Running](#Running)
* [Usage](#Usage)
* [Discussion](#Discussion)
* [License](#License)

##Running

### Docker
Using docker you can just run the application with docker-compose.
Place a reviews file as `reviews.json` into the root folder
```
docker-compose up
```
This will build the container and start up the service default on port 8080.

Additionally, a swagger ui is started that let's you explore the schema. 
To access it, open on `http://localhost:8081` in your browser, enter `./swagger.yml` in the field and hit `explore`.

**Note**: Due to some error in swagger ui, it is currently not possible to execute the requests
within swagger ui. 

### Sbt
You can run the application locally using sbt:
```
# will start the http server default on port 8080
sbt "run <file name>" 
```

##Usage

Use your favourite http application like httpie, curl or postman to call the service.

### Best rated products
E.g. using httpie:
```
http POST http://localhost:8080/amazon/best-rated start=15.10.2011 end=01.08.2013 limit=2 min_number_reviews=2
```

### Influencers
``` 
http POST http://localhost:8080/amazon/influencers type=influencer-reviews min_helpful_votes=1 helpful_percentage=0.3 search_phrases:='["love","hate"]'
```

##Discussion
I tried to keep the service as simple as possible. Therefore I chose http4s as a minimal http library.
Monix was chosen as a simple streaming library in order to not have to keep the whole file in 
memory but avoiding a full-blown solution such as spark. For a real-world application, it might however be necessary to 
switch to such a distributed stream processing.
The service is written in a purely-functional approach using tagless final.

**influencers** 

In the influencers case, I have to first identify the influencers and then find all their reviews.
Therefore I have to do a second pass over the reviews file. In monix one can't reuse a group stream,
so currently it parses the reviews file 1 + |influencers| times per request.

I left out the routing test cases (ServiceSpec) for the influencers case, as they would be 
similar to the best-rated case.    
  
### Assumptions
* Start & end dates in best rated request are both inclusive
* Results are finite lists, not infinite streams   
* Reading files multiple times is not an issue
* Reviews format is properly defined. No error correction is done, invalid lines are just dropped
* Param `type` in influencers request currently ignored since I only have the reviews

### Features
* Reviews file streamed upon every request
* Configurable
* Minimal request logging
* Dockerised 
* Swagger schema definition

### Code structure
```
Main.scala # application entrypoint
api/ # http api
config/ # application configuration
model/ # domain model
analytics/ # data analytics 
```

### TODOs
* Depending on the size of the data, it might be necessary to use sth like spark, because group-by streams 
  can cause the application to blow up at some point
* Error handling is rather basic and could be improved
* More significant tests should be written 
* Tests are currently a bit boilerplaty, this could be reduced by writing some test utilities like generators
* Currently there is only one model. If the application gets more complex, it might be useful to separate
  between an external api model, an internal business model, and a persistency model.
* Reviews store (currently via a file) should be abstracted, so that a database could be used.
* Add metrics 

##License
MIT License

Copyright (c) 2019 Jan Schulte

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
