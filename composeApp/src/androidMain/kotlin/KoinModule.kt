import com.github.nsk80.composesample.StickManGameScreenModel
import org.koin.dsl.module

val koinModule = module {
    single  { StickManGameScreenModel() }
}
