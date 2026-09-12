#ifndef CUCUMBER_PRETTY_FORMATTER_INTERFERENCE_INTERCEPTOR_HPP
#define CUCUMBER_PRETTY_FORMATTER_INTERFERENCE_INTERCEPTOR_HPP

#include <functional>
#include <initializer_list>
#include <ostream>
#include <sstream>
#include <streambuf>
#include <string_view>
#include <vector>

namespace cucumber::pretty_formatter
{
    class InterferenceInterceptor
    {
    public:
        using Reporter = std::function<void(std::string_view)>;

        explicit InterferenceInterceptor(Reporter reporter);
        InterferenceInterceptor(Reporter reporter, std::initializer_list<std::reference_wrapper<std::ostream>> streams);
        ~InterferenceInterceptor();

        InterferenceInterceptor(const InterferenceInterceptor&) = delete;
        InterferenceInterceptor& operator=(const InterferenceInterceptor&) = delete;
        InterferenceInterceptor(InterferenceInterceptor&&) = delete;
        InterferenceInterceptor& operator=(InterferenceInterceptor&&) = delete;

        void Acquire();
        void Release();
        void Bypass(const std::function<void()>& callback);

    private:
        struct Stream
        {
            Stream(std::ostream& output, std::streambuf* original)
                : output{ &output }
                , original{ original }
            {}

            std::ostream* output;
            std::streambuf* original;
        };

        void StartCapturing();
        void StopCapturing();

        Reporter reporter;
        std::vector<Stream> streams;
        std::ostringstream captured;
        bool acquired{ false };
    };
}

#endif
