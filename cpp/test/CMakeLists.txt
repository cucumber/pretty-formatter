add_executable(cucumber_pretty_formatter.test)

target_link_libraries(cucumber_pretty_formatter.test PUBLIC
    cucumber_pretty_formatter_lib
    GTest::gmock
    GTest::gmock_main
)

target_sources(cucumber_pretty_formatter.test PRIVATE
    EnumerateTestData.cpp
    EnumerateTestData.hpp
    TestAnsi.cpp
    TestLineBuilder.cpp
    TestPrettyFormatter.cpp
    TestTheme.cpp
)

target_include_directories(cucumber_pretty_formatter.test PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/../src/
)

target_compile_definitions(cucumber_pretty_formatter.test
    PRIVATE
        TESTDATA_SRC="${PROJECT_SOURCE_DIR}/../testdata/src"
)

include(GoogleTest)
gtest_discover_tests(cucumber_pretty_formatter.test)
