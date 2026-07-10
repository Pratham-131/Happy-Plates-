db.foods.find().forEach(function(food) {
  var encodedName = encodeURIComponent(food.name);
  var newUrl = "https://placehold.co/400x300/f97316/ffffff?text=" + encodedName;
  db.foods.updateOne({_id: food._id}, {$set: {imageUrl: newUrl}});
});
print("Done updating image URLs");