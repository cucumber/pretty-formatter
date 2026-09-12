#include "cucumber/pretty-formatter/InterferenceInterceptor.hpp"
#include <functional>
#include <initializer_list>
#include <iostream>
#include <utility>

namespace cucumber::pretty_formatter
{
    InterferenceInterceptor::InterferenceInterceptor(Reporter reporter)
        : InterferenceInterceptor{ std::move(reporter), { std::cout, std::cerr, std::clog } }
    {}

    InterferenceInterceptor::InterferenceInterceptor(Reporter reporter, std::initializer_list<std::reference_wrapper<std::ostream>> streams)
        : reporter{ std::move(reporter) }
    {
        this->streams.reserve(streams.size());
        for (const auto& output : streams)
        {
            this->streams.emplace_back(output.get(), output.get().rdbuf());
        }
    }

    InterferenceInterceptor::~InterferenceInterceptor()
    {
        Release();
    }

    void InterferenceInterceptor::Acquire()
    {
        if (acquired)
        {
            return;
        }

        StartCapturing();
        acquired = true;
    }

    void InterferenceInterceptor::Release()
    {
        if (!acquired)
        {
            return;
        }

        StopCapturing();
        acquired = false;

        const auto output = captured.str();
        captured.str("");
        captured.clear();
        if (!output.empty())
        {
            reporter(output);
        }
    }

    void InterferenceInterceptor::Bypass(const std::function<void()>& callback)
    {
        if (!acquired)
        {
            callback();
            return;
        }

        StopCapturing();
        callback();
        StartCapturing();
    }

    void InterferenceInterceptor::StartCapturing()
    {
        for (auto& stream : streams)
        {
            stream.output->rdbuf(captured.rdbuf());
        }
    }

    void InterferenceInterceptor::StopCapturing()
    {
        for (auto& stream : streams)
        {
            stream.output->rdbuf(stream.original);
        }
    }
}
