cmake_minimum_required(VERSION 3.18)
project("timfix")

add_library(timfix SHARED hook.cpp)
target_link_libraries(timfix log dl)
