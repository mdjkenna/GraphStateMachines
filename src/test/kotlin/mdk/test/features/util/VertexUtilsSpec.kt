package mdk.test.features.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import mdk.gsm.util.*

class VertexUtilsSpec : BehaviorSpec({
    
    Given("Concrete vertex implementations") {
        
        When("Creating a CharVertex") {
            val vertex = CharVertex('A')
            
            Then("It should have the correct id") {
                vertex.id shouldBe 'A'
            }
            
            Then("It should implement ICharVertex") {
                vertex.shouldBeInstanceOf<ICharVertex>()
            }
        }
        
        When("Creating a CharVertex with different characters") {
            val vertices = listOf(CharVertex('X'), CharVertex('Y'), CharVertex('Z'))
            
            Then("Each should have distinct ids") {
                vertices[0].id shouldBe 'X'
                vertices[1].id shouldBe 'Y'
                vertices[2].id shouldBe 'Z'
            }
        }
        
        When("Creating a ByteVertex") {
            val vertex = ByteVertex(42)
            
            Then("It should have the correct id") {
                vertex.id shouldBe 42.toByte()
            }
            
            Then("It should implement IByteVertex") {
                vertex.shouldBeInstanceOf<IByteVertex>()
            }
        }
        
        When("Creating ByteVertex with boundary values") {
            val minVertex = ByteVertex(Byte.MIN_VALUE)
            val maxVertex = ByteVertex(Byte.MAX_VALUE)
            val zeroVertex = ByteVertex(0)
            
            Then("Each should correctly store boundary values") {
                minVertex.id shouldBe Byte.MIN_VALUE
                maxVertex.id shouldBe Byte.MAX_VALUE
                zeroVertex.id shouldBe 0.toByte()
            }
        }
        
        When("Creating a ShortVertex") {
            val vertex = ShortVertex(1000)
            
            Then("It should have the correct id") {
                vertex.id shouldBe 1000.toShort()
            }
            
            Then("It should implement IShortVertex") {
                vertex.shouldBeInstanceOf<IShortVertex>()
            }
        }
        
        When("Creating ShortVertex with boundary values") {
            val minVertex = ShortVertex(Short.MIN_VALUE)
            val maxVertex = ShortVertex(Short.MAX_VALUE)
            val zeroVertex = ShortVertex(0)
            
            Then("Each should correctly store boundary values") {
                minVertex.id shouldBe Short.MIN_VALUE
                maxVertex.id shouldBe Short.MAX_VALUE
                zeroVertex.id shouldBe 0.toShort()
            }
        }
        
        When("Creating a StringVertex") {
            val vertex = StringVertex("test")
            
            Then("It should have the correct id") {
                vertex.id shouldBe "test"
            }
            
            Then("It should implement IStringVertex") {
                vertex.shouldBeInstanceOf<IStringVertex>()
            }
        }
        
        When("Creating an IntVertex") {
            val vertex = IntVertex(123)
            
            Then("It should have the correct id") {
                vertex.id shouldBe 123
            }
            
            Then("It should implement IIntVertex") {
                vertex.shouldBeInstanceOf<IIntVertex>()
            }
        }
        
        When("Creating a LongVertex") {
            val vertex = LongVertex(999L)
            
            Then("It should have the correct id") {
                vertex.id shouldBe 999L
            }
            
            Then("It should implement ILongVertex") {
                vertex.shouldBeInstanceOf<ILongVertex>()
            }
        }
    }
    
    Given("Data class equality for vertex implementations") {
        
        When("Comparing two CharVertex instances with same id") {
            val vertex1 = CharVertex('A')
            val vertex2 = CharVertex('A')
            
            Then("They should be equal") {
                vertex1 shouldBe vertex2
            }
            
            Then("They should have the same hashCode") {
                vertex1.hashCode() shouldBe vertex2.hashCode()
            }
        }
        
        When("Comparing two ByteVertex instances with same id") {
            val vertex1 = ByteVertex(5)
            val vertex2 = ByteVertex(5)
            
            Then("They should be equal") {
                vertex1 shouldBe vertex2
            }
        }
        
        When("Comparing two ShortVertex instances with same id") {
            val vertex1 = ShortVertex(100)
            val vertex2 = ShortVertex(100)
            
            Then("They should be equal") {
                vertex1 shouldBe vertex2
            }
        }
        
        When("Comparing vertices with different ids") {
            val charVertex1 = CharVertex('A')
            val charVertex2 = CharVertex('B')
            
            val byteVertex1 = ByteVertex(1)
            val byteVertex2 = ByteVertex(2)
            
            val shortVertex1 = ShortVertex(10)
            val shortVertex2 = ShortVertex(20)
            
            Then("They should not be equal") {
                (charVertex1 == charVertex2) shouldBe false
                (byteVertex1 == byteVertex2) shouldBe false
                (shortVertex1 == shortVertex2) shouldBe false
            }
        }
    }
})
