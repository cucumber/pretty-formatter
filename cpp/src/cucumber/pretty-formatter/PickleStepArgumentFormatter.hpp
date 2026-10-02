#ifndef CUCUMBER_PRETTY_FORMATTER_PICKLE_STEP_ARGUMENT_FORMATTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_PICKLE_STEP_ARGUMENT_FORMATTER_HPP

#include "cucumber/messages/PickleStepArgument.hpp"
#include "cucumber/pretty-formatter/Theme.hpp"
#include <cstddef>
#include <memory>
#include <ostream>

namespace cucumber::pretty_formatter
{
    struct PickleStepArgumentFormatter
    {
        PickleStepArgumentFormatter(std::ostream& stream, std::shared_ptr<Theme> theme, std::size_t indent);
        ~PickleStepArgumentFormatter() = default;

        PickleStepArgumentFormatter(const PickleStepArgumentFormatter&) = delete;
        PickleStepArgumentFormatter(PickleStepArgumentFormatter&&) = delete;
        PickleStepArgumentFormatter& operator=(const PickleStepArgumentFormatter&) = delete;
        PickleStepArgumentFormatter& operator=(PickleStepArgumentFormatter&&) = delete;

        void Format(const messages::PickleStepArgument& pickleStepArgument);

    private:
        std::ostream& stream;
        std::shared_ptr<Theme> theme;
        std::size_t indent;
    };
}

#endif
