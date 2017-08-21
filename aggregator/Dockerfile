FROM clojure:alpine

COPY . /code
WORKDIR /code

EXPOSE 7777

CMD ["lein", "repl", ":headless", ":host", "0.0.0.0", ":port", "7777"]
