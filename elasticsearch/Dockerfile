FROM docker.elastic.co/elasticsearch/elasticsearch:5.6.0

COPY config /usr/share/elasticsearch/config/

USER root
RUN chown -R elasticsearch:elasticsearch config

USER elasticsearch
