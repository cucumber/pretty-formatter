#include "cucumber/pretty-formatter/ProgressBarPrinter.hpp"
#include "cucumber/messages/Envelope.hpp"
#include <ostream>

namespace cucumber::pretty_formatter
{
    ProgressBarPrinter::ProgressBarPrinter(std::ostream& stream)
    {}

    void ProgressBarPrinter::Update(const messages::Envelope& envelope)
    {}
}
