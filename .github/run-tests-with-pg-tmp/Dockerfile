FROM postgres:14.1

COPY ephemeralpg-3.1.tar.gz /tmp

COPY entrypoint.sh /entrypoint.sh

RUN \
  apt-get update -y && \
  apt-get -y install make

RUN \
  apt-get update -y && \
  apt-get install -y gcc

RUN \
  cd /tmp && \
  tar xzf ephemeralpg-3.1.tar.gz && \
  cd ephemeralpg-3.1 && \
  make install

ENTRYPOINT ["/entrypoint.sh"]