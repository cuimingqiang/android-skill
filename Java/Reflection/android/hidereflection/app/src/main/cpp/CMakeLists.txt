cmake_minimum_required(VERSION 3.4.1)

file(GLOB sources *.cpp)

add_library(HideReflection SHARED ${sources})

find_library(log-lib log)

target_link_libraries(
        HideReflection
        log
)