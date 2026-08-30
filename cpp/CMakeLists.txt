# The VERSION here is a placeholder; the real version is in the VERSION file.
set(VERSION 1.0.0)

if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/VERSION")
    file(STRINGS "VERSION" LINES)
    list(GET LINES 0 VERSION)
endif()

# Crude Semver parsing
if("${VERSION}" MATCHES "^([^\\.]+)\\.([^\\.]+)\\.([^\\.]+)$")
    set(VER_MAJOR ${CMAKE_MATCH_1})
    set(VER_MINOR ${CMAKE_MATCH_2})
    set(VER_PATCH ${CMAKE_MATCH_3})
else()
    message(FATAL_ERROR "unable to parse version: ${VERSION}")
endif()

cmake_minimum_required(VERSION 3.21 FATAL_ERROR)
project(cucumber_pretty_formatter VERSION ${VERSION} LANGUAGES CXX)

# Options
if (POLICY CMP0077)
    # Allow CMake 3.13+ to override options when using FetchContent / add_subdirectory.
    cmake_policy(SET CMP0077 NEW)
endif ()

option(CUCUMBER_PRETTY_FORMATTER_BUILD_TESTS "Build the unit tests." Off)
option(CUCUMBER_PRETTY_FORMATTER_ENABLE_CLANG_TIDY "Run clang-tidy as part of the build." Off)
option(CUCUMBER_PRETTY_FORMATTER_ENABLE_COVERAGE "Enable compiler flags for code coverage measurements." Off)
option(CUCUMBER_PRETTY_FORMATTER_ENABLE_SANITIZERS "Enable compiler flags for the address and undefined behaviour sanitizers." Off)

if(CUCUMBER_PRETTY_FORMATTER_ENABLE_SANITIZERS AND CUCUMBER_PRETTY_FORMATTER_ENABLE_COVERAGE)
    message(FATAL_ERROR "CUCUMBER_PRETTY_FORMATTER_ENABLE_SANITIZERS and CUCUMBER_PRETTY_FORMATTER_ENABLE_COVERAGE are mutually exclusive.")
endif()

if(CUCUMBER_PRETTY_FORMATTER_ENABLE_CLANG_TIDY)
    find_program(CLANG_TIDY_PROGRAM clang-tidy REQUIRED)
endif()

if(CUCUMBER_PRETTY_FORMATTER_ENABLE_COVERAGE)
    find_program(GCOVR_PROGRAM gcovr REQUIRED)

    add_custom_target(
        generate-lcov-report
        COMMAND ${GCOVR_PROGRAM} --version
        COMMAND ${GCOVR_PROGRAM} --root ${CMAKE_CURRENT_SOURCE_DIR} --merge-lines --delete --exclude-unreachable-branches --exclude-throw-branches --exclude-noncode-lines --filter include/ --filter src/
            --txt-summary
            --lcov ${CMAKE_CURRENT_BINARY_DIR}/lcov.info
            --cobertura ${CMAKE_CURRENT_BINARY_DIR}/cobertura.xml --cobertura-pretty
            --markdown-summary ${CMAKE_CURRENT_BINARY_DIR}/coverage-summary.md
        WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
        COMMENT "Generating code coverage reports"
    )
endif()

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Compiler cache
find_program(CCACHE_PROGRAM ccache)
if(CCACHE_PROGRAM)
    set(CMAKE_CXX_COMPILER_LAUNCHER "${CCACHE_PROGRAM}")
    message(STATUS "Using ccache: ${CCACHE_PROGRAM}")
else()
    find_program(SCCACHE_PROGRAM sccache)
    if(SCCACHE_PROGRAM)
        set(CMAKE_CXX_COMPILER_LAUNCHER "${SCCACHE_PROGRAM}")
        message(STATUS "Using sccache: ${SCCACHE_PROGRAM}")
    endif()
endif()

# Configuration
include(GNUInstallDirs)

if (CMAKE_CXX_COMPILER_ID STREQUAL "MSVC")
    add_compile_definitions(_CRT_SECURE_NO_WARNINGS _SCL_SECURE_NO_WARNINGS)
endif()

# CMake dependencies
find_package(nlohmann_json REQUIRED)
find_package(fmt 10 REQUIRED) # version 10 or higher has fmt::println
if (NOT TARGET cucumber::messages)
    find_package(cucumber_messages REQUIRED)
endif()
if (NOT TARGET cucumber::query)
    find_package(cucumber_query REQUIRED)
endif()

# Target options
if(CUCUMBER_PRETTY_FORMATTER_ENABLE_CLANG_TIDY)
    set(CMAKE_CXX_CLANG_TIDY "${CLANG_TIDY_PROGRAM}")
endif()

if(CUCUMBER_PRETTY_FORMATTER_ENABLE_COVERAGE)
    add_compile_options(
        -g -O0 --coverage -fprofile-arcs -ftest-coverage -fno-inline
        $<$<COMPILE_LANGUAGE:CXX>:-fno-elide-constructors>
    )

    if (CMAKE_C_COMPILER_ID STREQUAL "GNU")
        link_libraries(gcov)
    else()
        add_link_options(--coverage)
    endif()
endif()

if(CUCUMBER_PRETTY_FORMATTER_ENABLE_SANITIZERS)
    if(CMAKE_CXX_COMPILER_ID STREQUAL "GNU")
        add_compile_options(-fsanitize=address -fsanitize=undefined)
        add_link_options(-fsanitize=address -fsanitize=undefined)
    endif()
endif()

# Targets
add_library(cucumber_pretty_formatter_lib)
add_library(cucumber::pretty_formatter ALIAS cucumber_pretty_formatter_lib)

target_sources(
    cucumber_pretty_formatter_lib
    PRIVATE
        include/cucumber/pretty-formatter/Ansi.hpp
        include/cucumber/pretty-formatter/Formatter.hpp
        include/cucumber/pretty-formatter/LineBuilder.hpp
        include/cucumber/pretty-formatter/PrettyPrinter.hpp
        include/cucumber/pretty-formatter/ProgressBarPrinter.hpp
        include/cucumber/pretty-formatter/ProgressPrinter.hpp
        include/cucumber/pretty-formatter/StepTextFormatter.hpp
        include/cucumber/pretty-formatter/SummaryPrinter.hpp
        src/cucumber/pretty-formatter/Ansi.cpp
        src/cucumber/pretty-formatter/LineBuilder.cpp
        src/cucumber/pretty-formatter/PrettyPrinter.cpp
        src/cucumber/pretty-formatter/ProgressBarPrinter.cpp
        src/cucumber/pretty-formatter/ProgressPrinter.cpp
        src/cucumber/pretty-formatter/StepTextFormatter.cpp
        src/cucumber/pretty-formatter/SummaryPrinter.cpp
        src/cucumber/pretty-formatter/SourceReferenceFormatter.cpp
        include/cucumber/pretty-formatter/SourceReferenceFormatter.hpp
)

target_include_directories(
    cucumber_pretty_formatter_lib
    PUBLIC
        $<BUILD_INTERFACE:${CMAKE_CURRENT_LIST_DIR}/include/>
        $<INSTALL_INTERFACE:${CMAKE_INSTALL_INCLUDEDIR}>
    PRIVATE
        ${CMAKE_CURRENT_LIST_DIR}/src/
)

target_link_libraries(
    cucumber_pretty_formatter_lib
    PUBLIC
        cucumber::messages
        cucumber::query
        fmt::fmt
)

set_target_properties(
    cucumber_pretty_formatter_lib
    PROPERTIES
        VERSION ${VERSION}
        SOVERSION ${VER_MAJOR}
        EXPORT_NAME pretty_formatter
        OUTPUT_NAME cucumber_pretty_formatter
)

# Installation support
include(CMakePackageConfigHelpers)

configure_package_config_file(
    "cmake/cucumber_pretty_formatter-config.cmake.in"
    "${PROJECT_BINARY_DIR}/cucumber_pretty_formatter-config.cmake"
    INSTALL_DESTINATION "${CMAKE_INSTALL_LIBDIR}/cmake/cucumber_pretty_formatter"
    PATH_VARS CMAKE_INSTALL_INCLUDEDIR CMAKE_INSTALL_LIBDIR
)

write_basic_package_version_file(
    "${PROJECT_BINARY_DIR}/cucumber_pretty_formatter-config-version.cmake"
    COMPATIBILITY AnyNewerVersion
)

install(
    TARGETS cucumber_pretty_formatter_lib
    EXPORT cucumber_pretty_formatter-targets
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
)

install(
    EXPORT cucumber_pretty_formatter-targets
    FILE cucumber_pretty_formatter-targets.cmake
    NAMESPACE cucumber::
    DESTINATION "${CMAKE_INSTALL_LIBDIR}/cmake/cucumber_pretty_formatter"
)

install(
    FILES
        "${PROJECT_BINARY_DIR}/cucumber_pretty_formatter-config.cmake"
        "${PROJECT_BINARY_DIR}/cucumber_pretty_formatter-config-version.cmake"
    DESTINATION "${CMAKE_INSTALL_LIBDIR}/cmake/cucumber_pretty_formatter"
)

install(
    DIRECTORY "${PROJECT_SOURCE_DIR}/include/"
    DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}
)

# Optional: tests
if(CUCUMBER_PRETTY_FORMATTER_BUILD_TESTS)
    find_package(GTest REQUIRED)
    include(CTest)
    enable_testing()
    add_subdirectory(test)
endif()
