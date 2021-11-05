FROM debian:bullseye

# Add essential tools to build
RUN DEBIAN_FRONTEND=noninteractive apt update && apt install --assume-yes --no-install-recommends build-essential zlib1g-dev wget ca-certificates && rm -rf /var/lib/apt/lists/*

# Folder where we install the Musl toolchain
RUN mkdir /toolchain

# Musl
RUN cd /toolchain && wget http://musl.cc/x86_64-linux-musl-native.tgz && tar xvzf x86_64-linux-musl-native.tgz && rm -rf x86_64-linux-musl-native.tgz

# Add musl-gcc binary to the path
ENV PATH="/toolchain/x86_64-linux-musl-native/bin:${PATH}"

# zlib
RUN mkdir /downloads
RUN cd /downloads && wget https://zlib.net/zlib-1.2.11.tar.gz && tar xvzf zlib-1.2.11.tar.gz && rm zlib-1.2.11.tar.gz
RUN cd /downloads/zlib-1.2.11 && CC=/toolchain/x86_64-linux-musl-native/bin/gcc ./configure --static --prefix=/toolchain/x86_64-linux-musl-native && make && make install && rm -rf /downloads/zlib-1.2.11