FROM debian:bullseye
RUN DEBIAN_FRONTEND=noninteractive apt update && apt install --assume-yes --no-install-recommends build-essential zlib1g-dev wget ca-certificates && rm -rf /var/lib/apt/lists/*
RUN mkdir /libs
RUN mkdir /downloads

# Musl
RUN cd /downloads && wget https://musl.libc.org/releases/musl-1.2.2.tar.gz && tar xvzf musl-1.2.2.tar.gz && rm musl-1.2.2.tar.gz
RUN cd /downloads/musl-1.2.2 && ./configure --disable-shared --prefix=/libs && make && make install

# zlib
ENV PATH="/libs/bin:${PATH}"
RUN cd /downloads && wget https://zlib.net/zlib-1.2.11.tar.gz && tar xvzf zlib-1.2.11.tar.gz && rm zlib-1.2.11.tar.gz
RUN cd /downloads/zlib-1.2.11 && CC=musl-gcc ./configure --static --prefix=/libs && make && make install

# libstdc++
RUN cp /usr/lib/gcc/x86_64-linux-gnu/10/libstdc++.a /libs/lib/


#
# tar xvzf musl-1.2.2.tar.gz
# cd musl-1.2.2

# ./configure --disable-shared --prefix=/libs
# make
# make install

# cd ..
# wget https://zlib.net/zlib-1.2.11.tar.gz
# tar xvzf zlib-1.2.11.tar.gz
# cd zlib-1.2.11

# export PATH=/libs/bin:$PATH
# export CC=musl-gcc
# ./configure --static --prefix=/libs

# cp /usr/lib/gcc/x86_64-linux-gnu/10/libstdc++.a /libs/lib/

# apt install wget
# wget https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-21.2.0/graalvm-ce-java11-linux-amd64-21.2.0.tar.gz
# tar xvzf
