import com.github.nsk90.composesample.StickManGameScreenModel
import org.koin.dsl.module

val koinModule = module {
    single  { StickManGameScreenModel() }
}
