import org.example.project.StickManGameScreenModel
import org.koin.dsl.module

val koinModule = module {
    single  { StickManGameScreenModel() }
}
