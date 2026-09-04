#ifndef CUCUMBER_PRETTY_FORMATTER_AS_UNARY_HPP
#define CUCUMBER_PRETTY_FORMATTER_AS_UNARY_HPP

#include <functional>
#include <utility>

namespace cucumber::pretty_formatter
{
    template<class Proj>
    auto AsUnary(Proj&& proj)
    {
        return [proj = std::forward<Proj>(proj)](const auto& item)
        {
            return std::invoke(proj, item);
        };
    }
}

#endif
