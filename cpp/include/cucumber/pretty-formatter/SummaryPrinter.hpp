#ifndef CUCUMBER_PRETTY_FORMATTER_SUMMARY_PRINTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_SUMMARY_PRINTER_HPP

#include "cucumber/messages/Envelope.hpp"
#include "cucumber/pretty-formatter/Formatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstdint>
#include <fmt/core.h>
#include <fmt/format.h>
#include <fmt/ostream.h>
#include <functional>
#include <memory>
#include <ostream>
#include <set>
#include <string>

namespace cucumber::pretty_formatter
{
    struct SummaryPrinter : Formatter
    {
        enum class Options : std::uint8_t
        {
            includeAttachments
        };

    protected:
        struct ProtectedConstructorTag
        {};

    public:
        SummaryPrinter(const ProtectedConstructorTag&, std::ostream& stream, std::shared_ptr<struct Theme> theme,
            std::function<std::string(std::string)> uriFormatter, std::set<enum Options> options);

        void Update(const messages::Envelope& envelope) override;

        struct Factory
        {
            Factory();

            Factory& Theme(std::shared_ptr<struct Theme> theme);
            Factory& RemoveUriPrefix(std::string prefix);
            Factory& Options(enum Options option, bool enabled = true);

            std::unique_ptr<Formatter> Build(std::ostream& stream) const;

        private:
            std::shared_ptr<struct Theme> theme;
            std::function<std::string(std::string)> uriFormatter;
            std::set<enum Options> options;
        };

    private:
        void PrintSummary();

        struct Data;
        std::unique_ptr<Data> data;

        struct Printer;
        std::unique_ptr<Printer> printer;
    };
}

#endif
