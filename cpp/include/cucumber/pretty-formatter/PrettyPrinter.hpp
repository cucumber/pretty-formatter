#ifndef CUCUMBER_PRETTY_FORMATTER_PRETTY_PRINTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_PRETTY_PRINTER_HPP

#include "cucumber/messages/Envelope.hpp"
#include "cucumber/pretty-formatter/Formatter.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstdint>
#include <functional>
#include <memory>
#include <ostream>
#include <set>
#include <string>

namespace cucumber::pretty_formatter
{
    struct PrettyPrinter : Formatter
    {
        enum class Options : std::uint8_t
        {
            includeFeatureLine,
            includeRuleLine,
            useStatusIcon,
            includeAttachments
        };

    protected:
        struct ProtectedConstructorTag
        {};

    public:
        PrettyPrinter(const ProtectedConstructorTag&, std::ostream& stream, std::shared_ptr<Theme> theme,
            std::function<std::string(std::string)> uriFormatter, std::set<Options> options);
        ~PrettyPrinter();

        void Update(const messages::Envelope& envelope) override;

        struct Factory
        {
            Factory();

            Factory& Theme(std::shared_ptr<Theme> theme);
            Factory& RemoveUriPrefix(std::string prefix);
            Factory& Options(enum Options option, bool enabled = true);
            std::unique_ptr<Formatter> Build(std::ostream& stream);

        private:
            std::shared_ptr<struct Theme> theme;
            std::function<std::string(std::string)> uriFormatter;
            std::set<enum Options> options;
        };

    private:
        std::shared_ptr<Theme> theme;
        std::shared_ptr<struct Data> data;
        std::unique_ptr<struct Printer> printer;
    };
}

#endif
