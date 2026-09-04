#ifndef CUCUMBER_PRETTY_FORMATTER_PICKLE_DOC_STRING_FORMATTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_PICKLE_DOC_STRING_FORMATTER_HPP

#include "cucumber/messages/PickleDocString.hpp"
#include "cucumber/pretty-formatter/LineBuilder.hpp"
#include <cstddef>
#include <memory>

namespace cucumber::pretty_formatter
{
    struct PickleDocStringFormatter
    {
        explicit PickleDocStringFormatter(std::size_t indent);

        void Format(LineBuilder& lineBuilder, const std::shared_ptr<const messages::PickleDocString>& docString);

    private:
        static constexpr auto* delimiter{ R"(""")" };

        std::size_t indent;
    };
}

#endif
