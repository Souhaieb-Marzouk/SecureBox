cmake_minimum_required(VERSION 3.22.1)
project(securebox_pump CXX)

add_library(securebox_pump SHARED packet_pump.cpp)
target_compile_options(securebox_pump PRIVATE -O2 -Wall -fvisibility=hidden)

find_library(log-lib log)
target_link_libraries(securebox_pump ${log-lib})
