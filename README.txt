mvn spring-boot:build-image
docker run --rm -p 7000:7000 rsocket-service:0.0.1-SNAPSHOT



Fire and Forget
rsc --debug --request --data='{"source":"rsc","message":"this is a request"}' --route fire-and-forget  tcp://localhost:7000
 

Request and Response
rsc --debug --request --data='{"source":"rsc","message":"hello"}' --route request-response  tcp://localhost:7000


Request Stream
rsc --debug --stream --data='{"source":"rsc","message" : "Get Events"}' --route request-stream  tcp://localhost:7000

Channel
rsc --debug --channel --data='{"source":"rsc","message":"Request #1"}' --route channel  tcp://localhost:7000


 