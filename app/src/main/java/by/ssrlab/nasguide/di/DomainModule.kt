package by.ssrlab.nasguide.di

import by.ssrlab.domain.repository.ui.UiDataProvider
import org.koin.dsl.module

val domainModule = module {
    single { UiDataProvider() }
}