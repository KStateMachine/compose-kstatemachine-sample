import com.sample.kstatemachine_compose_sample.StickManGameScreenModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {
    single  { StickManGameScreenModel() }
}
