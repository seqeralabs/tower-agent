FROM debian:bullseye

# Add essential tools to build
RUN DEBIAN_FRONTEND=noninteractive apt update && apt install --assume-yes --no-install-recommends build-essential zlib1g-dev wget ca-certificates && rm -rf /var/lib/apt/lists/*

# Folder where we install the libraries
RUN mkdir /libs

# Temporal folder to download libraries
RUN mkdir /downloads

# Musl
RUN cd /downloads && wget https://musl.libc.org/releases/musl-1.2.2.tar.gz && tar xvzf musl-1.2.2.tar.gz && rm musl-1.2.2.tar.gz
RUN cd /downloads/musl-1.2.2 && ./configure --disable-shared --prefix=/libs && make && make install && rm -rf /downloads/musl-1.2.2

# Add musl-gcc binary to the path
ENV PATH="/libs/bin:${PATH}"

# zlib
RUN cd /downloads && wget https://zlib.net/zlib-1.2.11.tar.gz && tar xvzf zlib-1.2.11.tar.gz && rm zlib-1.2.11.tar.gz
RUN cd /downloads/zlib-1.2.11 && CC=musl-gcc ./configure --static --prefix=/libs && make && make install && rm -rf /downloads/zlib-1.2.11

# libstdc++
RUN cp /usr/lib/gcc/x86_64-linux-gnu/10/libstdc++.a /libs/lib/