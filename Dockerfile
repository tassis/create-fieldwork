FROM gradle:9.2.1-jdk21@sha256:f1d5be114f4f16e780eee51a942449eaa98808887dddd5da4a5b608971c90aa4

WORKDIR /workspace

ENTRYPOINT ["gradle", "--no-daemon"]
CMD ["build"]
