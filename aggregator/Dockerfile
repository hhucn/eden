FROM clojure:alpine

COPY . /code
WORKDIR /code

EXPOSE 8080 7777
CMD ["lein", "ring", "server-headless"]
