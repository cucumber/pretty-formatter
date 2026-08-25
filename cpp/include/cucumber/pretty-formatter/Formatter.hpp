#ifndef CUCUMBER_PRETTY_FORMATTER_FORMATTER_HPP
#define CUCUMBER_PRETTY_FORMATTER_FORMATTER_HPP

namespace cucumber::pretty_formatter
{
    struct Formatter
    {
        Formatter() = default;
        virtual ~Formatter() = default;

        Formatter(const Formatter&) = delete;
        Formatter& operator=(const Formatter&) = delete;

        Formatter(Formatter&&) = default;
        Formatter& operator=(Formatter&&) = default;

        virtual void Update() = 0;
    };
}

#endif
