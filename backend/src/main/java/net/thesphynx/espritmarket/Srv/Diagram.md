@startuml
skinparam classAttributeIconSize 0

'================ ENUM =================
enum Role #Wheat {
  USER
  COURIER
  SELLER
  ORGANIZER
  SERVICE_PROVIDER
  DELIVERY 
  Recruteur
}

'================ USER =================
class User #Wheat {
  -id : Long
  -name : String
  -email : String
  -password : String
}

User -- Role 




'================ STORE =================
class Store #LightBlue{
  -id : Long
  -name : String
  -description : String
  -address : String
  -phone : String
  -rating : Double
}

User "1" -- "*" Store : owns
User "*" -- "*" Store : linked users

'================ CATEGORY =================
class Category #LightBlue{
  -id : Long
  -name : String
  -description : String
}

Store "1" -- "*" Category : has
Category "1" -- "*" Product

'================ PRODUCT =================
class Product #LightBlue{
  -id : Long
  -name : String
  -description : String
  -price : Double
  -stock : int
  -imageUrl : String
  -categoryId : Long
}

Store "1" -- "*" Product 
User "*" -- "*" Product : linked users

'================ REVIEW =================
class Review #LightBlue{
  -id : Long
  -comment : String
  -rating : int
}

User "1" -- "*" Review 
Product "1" -- "*" Review 

'================ ENUM =================

enum ProjectStatus #Plum {
  PLANNED
  IN_PROGRESS
  COMPLETED
  CANCELLED
}

ProjectStatus -- Project

enum ServiceStatus #Plum {
  AVAILABLE
  UNAVAILABLE
  BOOKED
}

ServiceStatus -- Service

'================ SERVICE =================
class Service #Plum {
  -id : Long
  -name : String
  -description : String
  -category : String
  -hourlyRate : Double
  -rating : Double
  -location : String
}

' Only users with SERVICE_PROVIDER role can create services
User "1" -- "*" Service 

'================ PROJECT =================
class Project #Plum {
  -id : Long
  -title : String
  -details : String
  -startDate : Date
  -estimatedEndDate : Date
  -endDate : Date
  -budget : Double
  -priority : String
}

' Many users can work on multiple projects
User "*" -- "*" Project

' Projects can involve multiple services
Project "*" -- "*" Service

'================ PARTNER =================
class Partner #Plum {
  -id : Long
  -name : String
  -contactInfo : String
  -role : Role
}

Partner "*" -- "*" Project
Partner "*" -- "*" Service
Partner "1" --> "*" User

'================ SERVICE REQUEST / BOOKING =================
class ServiceRequest #Plum {
  -id : Long
  -date : Date
  -duration : int
  -status : String
}

User "1" -- "*" ServiceRequest
Service "1" -- "*" ServiceRequest
Partner "1" -- "*" ServiceRequest

'================ REVIEW FOR SERVICE =================
class ServiceReview #Plum {
  -id : Long
  -comment : String
  -rating : int
}

User "1" -- "*" ServiceReview
Service "1" -- "*" ServiceReview


'================ ORDER =================
class Order #LightBlue{
  -id : Long
  -date : Date
  -status : String
  -totalAmount : Double
  -paymentMethod : String
  -shippingAddress : String
}

User "1" -- "*" Order 

'================ ORDER LINE =================
class OrderLine #LightBlue{
  -id : Long
  -quantity : int
  -price : Double
  -subtotal : Double
  -orderId : Long
  -productId : Long
}

Order "1" *-- "*" OrderLine
Product "1" -- "*" OrderLine

'================ DELIVERY =================
class Delivery #LightGreen {
  -id : Long
  -deliverytype : String
  -status : String
  -deliveryDate : Date
  -address : String


}

class MapTracking #LightGreen {
  -currentLocation : String
  -lastUpdate : DateTime
  -estimatedArrival : DateTime
}

class Courier #LightGreen{
  -id : int
  -name : String
  -status : String
}

Delivery "1" -- "1" MapTracking 
Courier "1" -- "*" Delivery 




Order "1" -- "1" Delivery


class Vehicle #LightGreen {
  -id : Long
  -type : String
  -plateNumber : String
  -capacity : Double
  -status : String

}
Courier "1" -- "1" Vehicle

'================ COMPLAINT =================
class Complaint {
  -id : Long
  -reason : String
  -status : String
}

Order "1" -- "*" Complaint 

'================ EVENT =================
class Event #Orange  {
  -id : Long
  -name : String
  -date : Date
  -location : String
}

class Stall #Orange {
  -id : Long
  -name : String
  -number : int
}

Event "1" -- "*" Stall 
User "0..1" -- "*" Stall 
'================ COLLABORATION =================
class Collaboration #Orange  {
  -id : Long
  -type : String
  -description : String
}

Event "1" -- "*" Collaboration

'================ RESERVATION =================
class Reservation #Orange {
  -id : Long
  -name : String
  -date : Date
}

Event "1" -- "*" Reservation

class EquipmentReservation #Orange {
  -id : Long
  -quantity : int
}

Reservation "1" -- "*" EquipmentReservation
Equipment "1" -- "*" EquipmentReservation
Stall "1" -- "*" EquipmentReservation

'================ EQUIPMENT =================
class Equipment #Orange {
  -id : Long
  -name : String
  -type : String
  -status : String
}

Event "1" -- "*" Equipment

'================ TICKET =================
class Ticket #Orange  {
  -id : Long
  -type : String
  -price : Double
}

Event "1" -- "*" Ticket
User "1" -- "*" Ticket






class PartnerCompany {

  -id : int
  -name : String
  -sector : String
  -contactEmail : String
  -partnershipStatus : String

 
}

class PartnershipAgreement {

  -id : int
  -startDate : Date
  -endDate : Date
  -status : String

  
}

class JobOffer {

  -id : int
  -title : String
  -description : String
  -type : String
  -publicationDate : Date
  -status : String

  
}

class Application {

  -id : int
  -applicationDate : Date
  -status : String
  -coverLetter : String
  -matchingScore : double

 
}

class Interview {

  -id : int
  -interviewDate : Date
  -type : String
  -result : String
  -status : String

  
}

' Inheritance


' Relationships
PartnerCompany "1" -- "0.." JobOffer
PartnerCompany "1" -- "0.." PartnershipAgreement
PartnerCompany "1" -- "*" User
Application "*" -- "1" User

JobOffer "1" -- "0.." Application
Application "1" -- "0..1" Interview
@enduml